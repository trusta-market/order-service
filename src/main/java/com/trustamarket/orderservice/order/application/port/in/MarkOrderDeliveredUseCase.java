package com.trustamarket.orderservice.order.application.port.in;

import java.util.UUID;

// SHIPPING → DELIVERED. delivery.completed Kafka 이벤트가 트리거.
public interface MarkOrderDeliveredUseCase {
    void markDelivered(UUID orderId);
}
