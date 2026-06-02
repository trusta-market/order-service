package com.trustamarket.orderservice.order.application.service.reconciliation;

import com.trustamarket.common.event.Events;
import com.trustamarket.common.event.OutboxEvent;
import com.trustamarket.orderservice.order.application.event.messaging.OrderEventTypes;
import com.trustamarket.orderservice.order.application.event.messaging.OrderPaidMessage;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.PaymentReconciliationRepository;
import com.trustamarket.orderservice.order.application.port.out.ReconciliationAlertPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.UsageStatus;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import com.trustamarket.orderservice.order.domain.model.ReconciliationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

// Scheduler 가 fetch 한 reconciliation row 1건을 처리.
// Feign 호출은 tx 밖, DB UPDATE 만 짧은 tx 안에서 — HikariCP connection 점유 시간 최소화.
//
// 결과 분기:
//   DEDUCTED      → order.markPaid() + outbox + reconciliation.markDone
//   INSUFFICIENT  → order.rollbackPaymentRequest() + reconciliation.markDone
//   NOT_FOUND     → order.rollbackPaymentRequest() + reconciliation.markDone
//   getUsage 실패 → reconciliation.recordUnknown (백오프) — MAX 도달 시 GIVEN_UP + 알림
//
// 1-step saga 이므로 wallet 에 외부 변경 되돌리는 보상 트랜잭션 없음 — order 상태 복귀만.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationProcessor {

    private static final String DOMAIN_TYPE = "ORDER";

    private final WalletPaymentPort walletPaymentPort;
    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;
    private final PaymentReconciliationRepository reconciliationRepository;
    private final ReconciliationAlertPort alertPort;
    private final TransactionTemplate txTemplate;

    public void processOne(PaymentReconciliation reconciliation) {
        UUID orderIdValue = reconciliation.getOrderId();
        UsageStatus usage;
        try {
            // tx 밖에서 외부 Feign 호출.
            usage = walletPaymentPort.getUsage(orderIdValue);
        } catch (RuntimeException e) {
            log.warn("[Reconciliation] getUsage 실패 — orderId={}, retryCount={}, err={}",
                    orderIdValue, reconciliation.getRetryCount(), e.toString());
            handleUnknown(reconciliation, e.getMessage());
            return;
        }

        try {
            applyResult(reconciliation, usage);
        } catch (RuntimeException e) {
            log.error("[Reconciliation] applyResult 실패 — orderId={}, result={}",
                    orderIdValue, usage.result(), e);
            handleUnknown(reconciliation, e.getMessage());
        }
    }

    // wallet 결과 (DEDUCTED / INSUFFICIENT / NOT_FOUND) 별 분기.
    // order 상태 전이 + reconciliation.markDone 을 같은 트랜잭션 안에서 commit.
    private void applyResult(PaymentReconciliation reconciliation, UsageStatus usage) {
        OrderId orderId = OrderId.of(reconciliation.getOrderId());
        txTemplate.execute(status -> {
            switch (usage.result()) {
                case DEDUCTED -> applyDeducted(orderId);
                case INSUFFICIENT, NOT_FOUND -> applyRollback(orderId);
            }
            reconciliation.markDone(Instant.now());
            reconciliationRepository.save(reconciliation);
            return null;
        });
    }

    private void applyDeducted(OrderId orderId) {
        Order order = orderRepository.findByIdOrThrow(orderId);
        // 이미 PAID 면 멱등 처리 — outbox 중복 발행 방지.
        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }
        OrderStatus pre = order.getStatus();
        order.markPaid();
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);
        orderRepository.save(order);

        Events.trigger(OutboxEvent.of(
                DOMAIN_TYPE, order.getId().value(),
                OrderEventTypes.ORDER_PAID,
                OrderPaidMessage.of(
                        order.getId().value(),
                        order.getProduct().id(),
                        order.getSeller().id(),
                        order.getBuyer().id(),
                        order.getType().name())));
    }

    private void applyRollback(OrderId orderId) {
        Order order = orderRepository.findByIdOrThrow(orderId);
        // 이미 REQUESTED 면 멱등 처리.
        if (order.getStatus() == OrderStatus.REQUESTED) {
            return;
        }
        OrderStatus pre = order.getStatus();
        order.rollbackPaymentRequest();
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);
        orderRepository.save(order);
    }

    // getUsage 자체가 또 실패한 경우 — 백오프 후 재시도. MAX 도달 시 GIVEN_UP + 알림.
    private void handleUnknown(PaymentReconciliation reconciliation, String error) {
        Instant now = Instant.now();
        txTemplate.execute(status -> {
            reconciliation.recordUnknown(error, now);
            reconciliationRepository.save(reconciliation);
            return null;
        });
        if (reconciliation.getStatus() == ReconciliationStatus.GIVEN_UP) {
            try {
                alertPort.notifyGivenUp(
                        reconciliation.getOrderId(),
                        reconciliation.getRetryCount(),
                        reconciliation.getLastError()
                );
            } catch (RuntimeException e) {
                // best-effort — 알림 실패가 reconciliation 자체 흐름을 막지 않게.
                log.error("[Reconciliation] GIVEN_UP 알림 발송 실패 — orderId={}",
                        reconciliation.getOrderId(), e);
            }
        }
    }
}
