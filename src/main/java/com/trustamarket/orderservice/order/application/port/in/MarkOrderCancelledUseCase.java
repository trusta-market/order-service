package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.model.OrderId;

// CANCELLATION_PROCESSING → CANCELLATION_COMPLETED.
// wallet.cancellation.completed Kafka 이벤트가 트리거.
public interface MarkOrderCancelledUseCase {
    void markCancelled(OrderId orderId);
}
