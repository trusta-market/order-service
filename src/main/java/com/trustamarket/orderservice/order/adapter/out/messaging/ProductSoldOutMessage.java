package com.trustamarket.orderservice.order.adapter.out.messaging;

import com.trustamarket.orderservice.order.domain.model.Order;

import java.time.Instant;
import java.util.UUID;

// 주문 확정 직후 product 가 SOLD_OUT 으로 전이되도록 발행하는 이벤트.
// product-service 의 ProductSoldOutListener 가 구독.
public record ProductSoldOutMessage(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Instant soldAt
) {
    public static ProductSoldOutMessage from(Order order) {
        return new ProductSoldOutMessage(
                UUID.randomUUID(),
                order.getId().value(),
                order.getProduct().id(),
                Instant.now()
        );
    }
}
