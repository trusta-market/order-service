package com.trustamarket.orderservice.order.application.port.in;

import java.util.UUID;

// 결제 시작 — POST /api/orders/{id}/payment-intents
// REQUESTED → PAYMENT_PENDING → (Wallet sync 호출) → PAID (MVP는 sync 직결, PR 8 후 이벤트로 전환)
public interface RequestPaymentUseCase {

    void requestPayment(RequestPaymentCommand command);

    record RequestPaymentCommand(
            UUID orderId,
            UUID buyerId   // 권한 검증용 (== order.buyer.id)
    ) {}
}
