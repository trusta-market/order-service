package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.common.event.Events;
import com.trustamarket.common.event.OutboxEvent;
import com.trustamarket.orderservice.order.application.event.messaging.OrderEventTypes;
import com.trustamarket.orderservice.order.application.event.messaging.OrderPaidMessage;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository.InboxPurposeKey;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointRequest;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointResponse;
import com.trustamarket.orderservice.order.application.service.support.OrderAccessGuard;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.exception.InsufficientPointBalanceException;
import com.trustamarket.orderservice.order.domain.exception.WalletCommunicationException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// REQUESTED → PAYMENT_PENDING → (Wallet sync) → PAID
// Saga 분리: wallet feign call 이 tx 밖이라 connection hold X.
//   [tx1] order load + requestPayment() — PAYMENT_PENDING commit (짧음, ~50ms)
//   [no tx] walletPaymentPort.deduct() — Feign 호출 (응답 timeout 5초까지 wait, connection 없음)
//   [tx2] 성공: markPaid() + outbox publish (commit)
//          실패: rollbackPaymentRequest() — PAYMENT_PENDING → REQUESTED (보상)
//
// 보상 실패 시 (tx2 의 rollback 자체가 fail) order 가 PAYMENT_PENDING 으로 stuck.
// → 별도 reconciliation job 필요 (현재 미구현, follow-up).
//
// Idempotency-Key 검증: RequestPaymentCommand compact constructor 가 NotBlank 보장.
@Service
@RequiredArgsConstructor
public class RequestPaymentService implements RequestPaymentUseCase {

    private static final String DOMAIN_TYPE = "ORDER";

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;
    private final WalletPaymentPort walletPaymentPort;
    private final InboxRepository inboxRepository;
    private final TransactionTemplate txTemplate;

    @Override
    public void requestPayment(RequestPaymentCommand cmd) {
        // 멱등성 — atomic INSERT 시도. 이미 처리된 키면 false 반환 → no-op.
        // (REQUIRES_NEW 트랜잭션이라 충돌 시 부모 트랜잭션 영향 없음.)
        if (!inboxRepository.tryRecordIdempotencyKey(cmd.idempotencyKey(), InboxPurposeKey.REQUEST_PAYMENT)) {
            return;
        }

        OrderId orderId = OrderId.of(cmd.orderId());

        // ── [tx1] 짧은 DB UPDATE: REQUESTED → PAYMENT_PENDING ──
        Long totalAmount = txTemplate.execute(status -> {
            Order order = orderRepository.findByIdOrThrow(orderId);
            OrderAccessGuard.verifyBuyer(order, cmd.buyerId());

            OrderStatus pre = order.getStatus();
            order.requestPayment();
            historyRecorder.record(order.getId(), pre, order.getStatus(), null);
            orderRepository.save(order);
            return order.getTotalAmount().value();
        });

        // ── [no tx] Wallet Feign 호출 (DB connection 없음) ──
        DeductPointResponse res;
        try {
            res = walletPaymentPort.deduct(
                    new DeductPointRequest(cmd.orderId(), cmd.buyerId(), totalAmount)
            );
            if (res == null) {
                compensateToRequested(orderId);
                throw new WalletCommunicationException();
            }
        } catch (com.trustamarket.orderservice.order.domain.exception.OrderException e) {
            compensateToRequested(orderId);
            throw e;
        } catch (RuntimeException e) {
            compensateToRequested(orderId);
            throw new WalletCommunicationException(e);
        }

        if (!res.isSuccess()) {
            compensateToRequested(orderId);
            throw new InsufficientPointBalanceException(
                    totalAmount,
                    res.balance() == null ? 0 : res.balance(),
                    res.shortage()
            );
        }

        // ── [tx2] 짧은 DB UPDATE: PAYMENT_PENDING → PAID + outbox publish ──
        txTemplate.execute(status -> {
            Order order = orderRepository.findByIdOrThrow(orderId);
            OrderStatus prePaid = order.getStatus();
            order.markPaid();
            historyRecorder.record(order.getId(), prePaid, order.getStatus(), null);
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
            return null;
        });
    }

    // Saga 보상 — PAYMENT_PENDING → REQUESTED 복귀.
    // 본 트랜잭션은 항상 commit 시도. 실패 시 order 가 PAYMENT_PENDING 으로 stuck → reconciliation job 으로 복구 (follow-up).
    private void compensateToRequested(OrderId orderId) {
        txTemplate.execute(status -> {
            Order order = orderRepository.findByIdOrThrow(orderId);
            OrderStatus pre = order.getStatus();
            order.rollbackPaymentRequest();
            historyRecorder.record(order.getId(), pre, order.getStatus(), null);
            orderRepository.save(order);
            return null;
        });
    }
}
