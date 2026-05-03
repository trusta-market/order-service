package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

// 주문 생성 응답 — POST /api/v1/orders
public record CreateOrderResponse(
        UUID orderId,
        OrderStatus status,
        long totalAmount,
        Instant createdAt
) {
    public static CreateOrderResponse from(Order order) {
        return new CreateOrderResponse(
                order.getId().value(),
                order.getStatus(),
                order.getTotalAmount().value(),
                order.getCreatedAt()
        );
    }
}
