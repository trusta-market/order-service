package com.trustamarket.orderservice.order.adapter.in.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// delivery-service 의 `delivery.started` 토픽 페이로드. delivery 팀과 스키마 협의 필요.
public record DeliveryStartedMessage(
        UUID    eventId,           // 멱등성 키
        UUID    orderId,
        Instant startedAt
) {}
