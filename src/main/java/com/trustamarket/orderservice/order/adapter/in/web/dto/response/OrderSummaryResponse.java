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
    public OrderSummaryResponse {
        if (orderId == null || status == null) {
            throw new IllegalArgumentException("OrderSummaryResponse 필수 필드가 비었습니다.");
        }
        if (buyerName == null || buyerName.isBlank()
                || sellerName == null || sellerName.isBlank()
                || productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("표시 이름은 비어 있을 수 없습니다.");
        }
        if (totalAmount < 0) {
            throw new IllegalArgumentException("totalAmount must be >= 0");
        }
        // createdAt 은 JPA Auditing 영향 — null 허용
    }

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
