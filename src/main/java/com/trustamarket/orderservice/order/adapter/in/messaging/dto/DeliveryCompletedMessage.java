package com.trustamarket.orderservice.order.adapter.in.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// delivery-service 의 `delivery.completed` 토픽 페이로드.
public record DeliveryCompletedMessage(
        UUID    eventId,           // 멱등성 키
        UUID    orderId,
        Instant completedAt
) {}
