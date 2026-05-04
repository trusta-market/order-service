package com.trustamarket.orderservice.order.adapter.out.messaging;

import java.time.Instant;
import java.util.UUID;

// 주문 확정 직후 product 가 SOLD_OUT 으로 전이되도록 발행하는 이벤트.
// product-service 의 ProductSoldOutListener 가 구독.
public record ProductSoldOutMessage(
        UUID eventId,
        UUID orderId,
        UUID productId,
        Instant soldAt
) {}
