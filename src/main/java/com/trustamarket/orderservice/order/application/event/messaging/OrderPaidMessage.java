package com.trustamarket.orderservice.order.application.event.messaging;

import java.util.UUID;

public record OrderPaidMessage(
        UUID eventId,
        UUID orderId,
        UUID productId,
        UUID sellerId,
        UUID buyerId,
        String orderType
) {
    public static OrderPaidMessage of(UUID orderId, UUID productId, UUID sellerId, UUID buyerId, String orderType) {
        return new OrderPaidMessage(UUID.randomUUID(), orderId, productId, sellerId, buyerId, orderType);
    }
}
