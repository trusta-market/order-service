package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderType;

import java.util.UUID;

// 주문 생성 — POST /api/orders
// Buyer/Seller/Product snapshot 정보는 클라이언트가 직접 전달 (cross-domain 호출 최소화)
public interface CreateOrderUseCase {

    OrderId createOrder(CreateOrderCommand command);

    record CreateOrderCommand(
            UUID buyerId, String buyerName,
            UUID sellerId, String sellerName,
            UUID productId, String productName, long productPrice,
            OrderType type,
            long shippingFee
    ) {}
}
