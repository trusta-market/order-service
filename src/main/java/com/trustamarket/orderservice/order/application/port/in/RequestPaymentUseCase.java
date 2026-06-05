package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;

import java.util.UUID;

// 결제 시작 — POST /api/orders/{id}/payment-intents
// REQUESTED → PAYMENT_PENDING → (Wallet sync 호출) → PAID (MVP는 sync 직결, 후속 PR에 이벤트로 전환)
public interface RequestPaymentUseCase {

    void requestPayment(RequestPaymentCommand command);

    record RequestPaymentCommand(
            UUID orderId,
            UUID buyerId,             // 권한 검증용 (== order.buyer.id)
            String idempotencyKey     // Idempotency-Key 헤더 — 중복 결제 차단. wallet 으로도 그대로 전달되므로 UUID 형식 강제.
    ) {
        public RequestPaymentCommand {
            if (orderId == null) throw new InvalidIdException("orderId");
            if (buyerId == null) throw new InvalidIdException("buyerId");
            // 진입점 (controller 외 테스트/배치 등) 무관하게 record 가 직접 보장.
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new InvalidIdException("idempotencyKey");
            }
            try {
                UUID.fromString(idempotencyKey);
            } catch (IllegalArgumentException ignored) {
                throw new InvalidIdException("idempotencyKey");
            }
        }
    }
}
