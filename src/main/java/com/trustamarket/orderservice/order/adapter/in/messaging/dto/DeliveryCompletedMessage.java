package com.trustamarket.orderservice.order.adapter.in.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// delivery-service 의 `delivery.completed` 토픽 페이로드. delivery 의 DeliveryCompletedKafkaEvent 와 schema 일치.
public record DeliveryCompletedMessage(
        UUID    eventId,           // 멱등성 키 (delivery 의 publisher 가 UUID.randomUUID 로 생성)
        UUID    orderId,
        UUID    deliveryId,
        Instant completedAt
) {}
