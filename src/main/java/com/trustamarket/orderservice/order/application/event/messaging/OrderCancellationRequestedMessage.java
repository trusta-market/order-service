package com.trustamarket.orderservice.order.application.event.messaging;

import java.time.Instant;
import java.util.UUID;

// wallet-service 의 `order.cancellation.requested` 토픽 페이로드 — 양 팀 합의 스키마.
// wallet 측은 escrow 차감 + buyer 지갑 복구 후 wallet.cancellation.completed 응답.
public record OrderCancellationRequestedMessage(
        UUID    eventId,           // 멱등성 키
        UUID    orderId,
        UUID    buyerId,
        long    cancelledAmount,
        Instant cancelledAt
) {
    public static OrderCancellationRequestedMessage of(
            UUID orderId, UUID buyerId, long cancelledAmount, Instant cancelledAt) {
        return new OrderCancellationRequestedMessage(
                UUID.randomUUID(), orderId, buyerId, cancelledAmount, cancelledAt);
    }
}
