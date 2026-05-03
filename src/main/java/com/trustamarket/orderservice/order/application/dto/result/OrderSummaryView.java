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
