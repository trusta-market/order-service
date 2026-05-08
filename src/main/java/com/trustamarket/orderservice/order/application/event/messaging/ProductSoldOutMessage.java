package com.trustamarket.orderservice.order.application.event.messaging;

import java.time.Instant;
import java.util.UUID;

// 주문 확정 직후 product 가 SOLD_OUT 으로 전이되도록 발행하는 이벤트.
// product-service 의 ProductSoldOutListener 가 구독.
//
// 위치: application/event/messaging — adapter 의존 차단.
public record ProductSoldOutMessage(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Instant soldAt
) {
    public static ProductSoldOutMessage of(UUID orderId, UUID productId, Instant soldAt) {
        return new ProductSoldOutMessage(UUID.randomUUID(), orderId, productId, soldAt);
    }
}
