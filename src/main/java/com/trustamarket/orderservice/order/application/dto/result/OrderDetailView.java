package com.trustamarket.orderservice.order.application.dto.result;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import com.trustamarket.orderservice.order.domain.model.Reason;

import java.time.Instant;
import java.util.UUID;

// 주문 단건 상세 read-model — Get/Cancel/Confirm/Return 등 단건 응답에 사용.
public record OrderDetailView(
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
    public OrderDetailView {
        if (orderId == null || buyerId == null || sellerId == null || productId == null) {
            throw new IllegalArgumentException("id fields must not be null");
        }
        if (type == null || status == null) {
            throw new IllegalArgumentException("type/status must not be null");
        }
        if (productPrice < 0 || shippingFee < 0 || totalAmount < 0) {
            throw new IllegalArgumentException("amount fields must be >= 0");
        }
        // createdAt/updatedAt 은 JPA Auditing 이 commit 시점에 채움 — read-model 시점엔 null 가능
    }

    public static OrderDetailView from(Order order) {
        return new OrderDetailView(
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
