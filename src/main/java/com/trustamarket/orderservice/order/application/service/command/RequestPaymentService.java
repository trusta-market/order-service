package com.trustamarket.orderservice.order.application.service.command;

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
import org.springframework.transaction.annotation.Transactional;

// REQUESTED → PAYMENT_PENDING → (Wallet sync) → PAID
// 잔액 부족 시: throw → @Transactional 자동 롤백 → REQUESTED 복귀 → 클라가 충전 후 재시도
//
// Idempotency-Key 검증: RequestPaymentCommand compact constructor 가 NotBlank 보장.
// 따라서 본 메서드는 cmd.idempotencyKey() 가 항상 non-blank 라고 가정.
@Service
@RequiredArgsConstructor
public class RequestPaymentService implements RequestPaymentUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;
    private final WalletPaymentPort walletPaymentPort;
    private final InboxRepository inboxRepository;

    @Override
    @Transactional
    public void requestPayment(RequestPaymentCommand cmd) {
        // 멱등성 체크 — 동일 (idempotency_key, REQUEST_PAYMENT) 로 이미 처리됐으면 no-op.
        if (inboxRepository.existsByIdempotencyKey(cmd.idempotencyKey(), InboxPurposeKey.REQUEST_PAYMENT)) {
            return;
        }

        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));
        OrderAccessGuard.verifyBuyer(order, cmd.buyerId());

        OrderStatus pre = order.getStatus();
        order.requestPayment();   // REQUESTED → PAYMENT_PENDING
        historyRecorder.record(order.getId(), pre, order.getStatus(), null);

        DeductPointResponse res = callWallet(cmd, order);
        if (!res.isSuccess()) {
            throw new InsufficientPointBalanceException(
                    order.getTotalAmount().value(),
                    res.balance() == null ? 0 : res.balance(),
                    res.shortage()
            );
        }

        // Wallet 성공 → 즉시 PAID 전이.
        OrderStatus prePaid = order.getStatus();
        order.markPaid();
        historyRecorder.record(order.getId(), prePaid, order.getStatus(), null);

        orderRepository.save(order);

        // 멱등성 키 기록 — 같은 트랜잭션에 묶임 (commit 후 동일 키 재호출 시 위 체크로 short-circuit).
        inboxRepository.recordIdempotencyKey(cmd.idempotencyKey(), InboxPurposeKey.REQUEST_PAYMENT);
        // 정산 발행은 ConfirmOrderService 로 이동 (구매 확정 후 분배).
    }

    // Wallet 동기 호출 + null 응답/예외를 도메인 예외로 일관 변환.
    // 도메인 예외 (OrderException 상속) 는 검증/비즈니스 오류라 그대로 전파.
    // 그 외 RuntimeException (통신 장애, NPE 등) 만 WalletCommunicationException 으로 변환.
    private DeductPointResponse callWallet(RequestPaymentCommand cmd, Order order) {
        try {
            DeductPointResponse res = walletPaymentPort.deduct(
                    new DeductPointRequest(cmd.orderId(), cmd.buyerId(), order.getTotalAmount().value())
            );
            if (res == null) {
                throw new WalletCommunicationException();
            }
            return res;
        } catch (com.trustamarket.orderservice.order.domain.exception.OrderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new WalletCommunicationException(e);
        }
    }
}
