package com.trustamarket.orderservice.order.adapter.in.web.dto.response;

import com.trustamarket.orderservice.order.application.dto.result.OrderDetailView;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;

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
    public static OrderDetailResponse from(OrderDetailView v) {
        return new OrderDetailResponse(
                v.orderId(),
                v.buyerId(), v.buyerName(),
                v.sellerId(), v.sellerName(),
                v.productId(), v.productName(), v.productPrice(),
                v.type(),
                v.status(),
                v.shippingFee(),
                v.totalAmount(),
                v.cancelReason(),
                v.returnReason(),
                v.rejectReason(),
                v.confirmedAt(),
                v.createdAt(),
                v.updatedAt()
        );
    }
}
