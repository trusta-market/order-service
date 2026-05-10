package com.trustamarket.orderservice.order.application.port.in;

import java.util.UUID;

// PAID → SHIPPING. delivery.started Kafka 이벤트가 트리거.
public interface MarkOrderShippedUseCase {
    void markShipped(UUID orderId);
}
