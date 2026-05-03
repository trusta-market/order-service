package com.trustamarket.orderservice.order.application.dto.result;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;

import java.time.Instant;
import java.util.UUID;

// 주문 목록 항목 read-model — me / me/purchases / me/sales / admin list / admin search.
public record OrderSummaryView(
        UUID orderId,
        String buyerName,
        String sellerName,
        String productName,
        OrderStatus status,
        long totalAmount,
        Instant createdAt
) {
    public OrderSummaryView {
        if (orderId == null || status == null) {
            throw new IllegalArgumentException("OrderSummaryView 필수 필드가 비었습니다.");
        }
        if (buyerName == null || buyerName.isBlank()
                || sellerName == null || sellerName.isBlank()
                || productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("표시 이름은 비어 있을 수 없습니다.");
        }
        if (totalAmount < 0) {
            throw new IllegalArgumentException("totalAmount must be >= 0");
        }
        // createdAt 은 JPA Auditing 이 commit 시점에 채움 — read-model 시점엔 null 가능
    }

    public static OrderSummaryView from(Order order) {
        return new OrderSummaryView(
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
