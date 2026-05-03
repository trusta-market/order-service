package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
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
    public static OrderSummaryResponse from(OrderSummaryView v) {
        return new OrderSummaryResponse(
                v.orderId(),
                v.buyerName(),
                v.sellerName(),
                v.productName(),
                v.status(),
                v.totalAmount(),
                v.createdAt()
        );
    }
}
