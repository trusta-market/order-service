package com.trustamarket.orderservice.order.application.dto.result;

import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;

import java.util.UUID;

// 상태 이력 read-model — admin status-history 응답.
public record OrderStatusHistoryView(
        UUID id,
        OrderStatus prevStatus,
        OrderStatus nextStatus,
        String reason
) {
    public static OrderStatusHistoryView from(OrderStatusHistory h) {
        return new OrderStatusHistoryView(
                h.id(),
                h.prevStatus(),
                h.nextStatus(),
                h.reason() == null ? null : h.reason().value()
        );
    }
}
