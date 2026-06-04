package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.common.event.Events;
import com.trustamarket.common.event.OutboxEvent;
import com.trustamarket.orderservice.order.application.event.messaging.OrderEventTypes;
import com.trustamarket.orderservice.order.application.event.messaging.OrderPaidMessage;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import com.trustamarket.orderservice.order.application.port.out.InboxRepository.InboxPurposeKey;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.PaymentReconciliationRepository;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointRequest;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointResponse;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.UsageStatus;
import com.trustamarket.orderservice.order.application.service.support.OrderAccessGuard;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.exception.InsufficientPointBalanceException;
import com.trustamarket.orderservice.order.domain.exception.PaymentVerificationPendingException;
import com.trustamarket.orderservice.order.domain.exception.WalletCommunicationException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

// REQUESTED → PAYMENT_PENDING → (Wallet sync) → PAID
// Saga 분리: wallet feign call 이 tx 밖이라 connection hold X.
//   [tx1]    order load + requestPayment() — PAYMENT_PENDING commit (짧음, ~50ms)
//   [no tx]  walletPaymentPort.deduct() — Feign 호출 (응답 timeout 5초까지 wait, connection 없음)
//   [tx2]    성공: markPaid() + outbox publish (commit)
//            실패: rollbackToRequested() — PAYMENT_PENDING → REQUESTED (상태 복귀)
//
// 비정상 응답 (timeout / 5xx) 처리:
//   walletPaymentPort.getUsage(orderId) 로 결과 재확인
//     DEDUCTED     → markPaid()                                    (wallet 은 차감했고 응답만 손실)
//     INSUFFICIENT → rollbackToRequested + InsufficientPointBalanceException
//     NOT_FOUND    → rollbackToRequested + WalletCommunicationException
//   getUsage 자체도 실패 → reconciliation 큐 등록 + PaymentVerificationPendingException
//                          → 사용자에게 "결제 처리 중" 응답
//                          → @Scheduled 가 백오프 (30s/60s/120s) 로 재시도
//
// 1-step saga 이므로 토스 환전 사례의 "출금 취소" 같은 보상 트랜잭션 없음.
// NOT_FOUND / INSUFFICIENT 는 wallet 에 외부 변경이 없으므로 order 상태만 REQUESTED 로 복귀.
//
// Idempotency-Key 검증: RequestPaymentCommand compact constructor 가 NotBlank 보장.
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestPaymentService implements RequestPaymentUseCase {

    private static final String DOMAIN_TYPE = "ORDER";

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;
    private final WalletPaymentPort walletPaymentPort;
    private final InboxRepository inboxRepository;
    private final PaymentReconciliationRepository reconciliationRepository;
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
                throw new WalletCommunicationException();
            }
        } catch (WalletCommunicationException e) {
            // 비정상 응답 (timeout / 5xx) — Adapter 가 모두 이 예외로 통일. getUsage 재확인 후 분기.
            handleUnknownResult(orderId, totalAmount, e);
            return;
        } catch (com.trustamarket.orderservice.order.domain.exception.OrderException e) {
            // 기타 도메인 예외 (검증 실패 등) — 결과 확인 무의미. 즉시 상태 복귀.
            rollbackToRequested(orderId);
            throw e;
        } catch (RuntimeException e) {
            // 예상 못 한 런타임 — 안전망. getUsage 재확인 후 분기.
            handleUnknownResult(orderId, totalAmount, e);
            return;
        }

        if (!res.isSuccess()) {
            // 정상 응답이지만 잔액 부족 — 즉시 상태 복귀.
            rollbackToRequested(orderId);
            throw new InsufficientPointBalanceException(
                    totalAmount,
                    res.balance() == null ? 0 : res.balance(),
                    res.shortage()
            );
        }

        // ── [tx2] 성공: PAID + outbox ──
        markPaidAndPublish(orderId);
    }

    // saga catch 안에서 wallet 결과를 재확인하는 분기.
    // - DEDUCTED     → markPaid + outbox (성공으로 처리)
    // - INSUFFICIENT → rollbackToRequested + InsufficientPointBalanceException (잔액 부족)
    // - NOT_FOUND    → rollbackToRequested + WalletCommunicationException (요청이 wallet 에 안 닿음)
    // - getUsage 자체 실패 → reconciliation 큐 등록 + PaymentVerificationPendingException (재시도 위임)
    private void handleUnknownResult(OrderId orderId, long totalAmount, RuntimeException original) {
        UsageStatus usage;
        try {
            usage = walletPaymentPort.getUsage(orderId.value());
        } catch (RuntimeException verifyEx) {
            // 결과 확인도 실패 — DB queue 에 등록하고 사용자에게 "결제 처리 중" 응답.
            reconciliationRepository.enqueueIfAbsent(
                    PaymentReconciliation.enqueue(orderId.value(), Instant.now())
            );
            throw new PaymentVerificationPendingException(orderId.value(), verifyEx);
        }

        switch (usage.result()) {
            case DEDUCTED -> markPaidAndPublish(orderId);
            case INSUFFICIENT -> {
                rollbackToRequested(orderId);
                throw new InsufficientPointBalanceException(
                        totalAmount,
                        usage.balance() == null ? 0 : usage.balance(),
                        usage.shortage()
                );
            }
            case NOT_FOUND -> {
                rollbackToRequested(orderId);
                throw new WalletCommunicationException(original);
            }
            // 새 Result 값 추가 시 컴파일 통과돼도 default 가 즉시 실패시켜 회귀를 조기에 드러낸다.
            default -> throw new IllegalStateException(
                    "Unexpected wallet usage result: " + usage.result());
        }
    }

    // tx2 — PAYMENT_PENDING → PAID + outbox publish.
    private void markPaidAndPublish(OrderId orderId) {
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

    // SAGA 상태 복귀 (보상 트랜잭션 아님) — PAYMENT_PENDING → REQUESTED.
    // 1-step saga 라 wallet 에 되돌릴 외부 변경 없음. order 상태만 복귀.
    // 본 트랜잭션이 깨지면 reconciliation 큐로 위임 — scheduler 가 백오프 후 getUsage 재시도하면서 결국 정리.
    private void rollbackToRequested(OrderId orderId) {
        try {
            txTemplate.execute(status -> {
                Order order = orderRepository.findByIdOrThrow(orderId);
                OrderStatus pre = order.getStatus();
                order.rollbackPaymentRequest();
                historyRecorder.record(order.getId(), pre, order.getStatus(), null);
                orderRepository.save(order);
                return null;
            });
        } catch (RuntimeException e) {
            log.error("[saga] rollbackToRequested 실패 — reconciliation 큐로 위임. orderId={}", orderId, e);
            reconciliationRepository.enqueueIfAbsent(
                    PaymentReconciliation.enqueue(orderId.value(), Instant.now())
            );
        }
    }
}
