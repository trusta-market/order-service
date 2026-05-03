package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.application.dto.result.CreateOrderResult;
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
    public static CreateOrderResponse from(CreateOrderResult result) {
        return new CreateOrderResponse(
                result.orderId(),
                result.status(),
                result.totalAmount(),
                result.createdAt()
        );
    }
}
