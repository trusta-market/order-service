package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

// 주문 목록 항목 응답 — GET /api/v1/orders/me, /me/purchases, /me/sales, /api/v1/admin/orders, /admin/orders/search
public record OrderSummaryResponse(
        UUID orderId,
        String buyerName,
        String sellerName,
        String productName,
        OrderStatus status,
        long totalAmount,
        Instant createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId().value(),
                order.getBuyer().name(),
                order.getSeller().name(),
                order.getProduct().name(),
                order.getStatus(),
                order.getTotalAmount().value(),
                order.getCreatedAt()
        );
    }
}
