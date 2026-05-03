package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import com.trustamarket.orderservice.order.domain.model.Reason;

import java.time.Instant;
import java.util.UUID;

// 주문 상세 응답 — GET /api/v1/orders/{id}
public record OrderDetailResponse(
        UUID orderId,
        UUID buyerId, String buyerName,
        UUID sellerId, String sellerName,
        UUID productId, String productName, long productPrice,
        OrderType type,
        OrderStatus status,
        long shippingFee,
        long totalAmount,
        String cancelReason,
        String returnReason,
        String rejectReason,
        Instant confirmedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId().value(),
                order.getBuyer().id(), order.getBuyer().name(),
                order.getSeller().id(), order.getSeller().name(),
                order.getProduct().id(), order.getProduct().name(), order.getProduct().price().value(),
                order.getType(),
                order.getStatus(),
                order.getShippingFee().value(),
                order.getTotalAmount().value(),
                reasonValue(order.getCancelReason()),
                reasonValue(order.getReturnReason()),
                reasonValue(order.getRejectReason()),
                order.getConfirmedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private static String reasonValue(Reason reason) {
        return reason == null ? null : reason.value();
    }
}
