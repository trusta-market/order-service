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
    public CreateOrderResponse {
        if (orderId == null) throw new IllegalArgumentException("orderId");
        if (status == null) throw new IllegalArgumentException("status");
        if (totalAmount < 0) throw new IllegalArgumentException("totalAmount must be >= 0");
        // createdAt 은 JPA Auditing 의 lifecycle 영향 받음 — null 허용
    }

    public static CreateOrderResponse from(CreateOrderResult result) {
        return new CreateOrderResponse(
                result.orderId(),
                result.status(),
                result.totalAmount(),
                result.createdAt()
        );
    }
}
