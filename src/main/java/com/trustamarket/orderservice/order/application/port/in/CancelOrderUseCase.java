package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;

import java.util.UUID;

// 주문 취소 — POST /api/orders/{id}/cancellations
// REQUESTED/PAYMENT_PENDING → CANCELLED, PAID → REFUND_PROCESSING
// markRefunded(환불 완료 처리)는 MVP scope 외 (TODO)
public interface CancelOrderUseCase {

    void cancel(CancelOrderCommand command);

    record CancelOrderCommand(
            UUID orderId,
            UUID actorId,    // buyer 본인 또는 ADMIN
            String reason
    ) {
        public CancelOrderCommand {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (actorId == null) throw new InvalidIdException("actorId");
            if (reason == null || reason.isBlank()) throw new InvalidReasonException();
        }
    }
}
