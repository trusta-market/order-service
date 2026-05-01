package com.trustamarket.orderservice.order.application.port.in;

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
    ) {}
}
