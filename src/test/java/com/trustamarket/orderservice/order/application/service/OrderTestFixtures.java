package com.trustamarket.orderservice.order.application.service;

import com.trustamarket.orderservice.order.domain.model.Buyer;
import com.trustamarket.orderservice.order.domain.model.Money;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import com.trustamarket.orderservice.order.domain.model.Product;
import com.trustamarket.orderservice.order.domain.model.Reason;
import com.trustamarket.orderservice.order.domain.model.Seller;

import java.util.UUID;

// 테스트 공용 fixture — Order 인스턴스 생성 헬퍼
public final class OrderTestFixtures {

    public static final long PRICE = 100_000L;
    public static final long SHIPPING = 3_000L;

    private OrderTestFixtures() {}

    public static Order requestedOrder(UUID buyerId, UUID sellerId) {
        return Order.create(
                Buyer.of(buyerId, "구매자"),
                Seller.of(sellerId, "판매자"),
                Product.of(UUID.randomUUID(), "상품", Money.of(PRICE)),
                OrderType.LOW,
                Money.of(SHIPPING)
        );
    }

    public static Order requestedOrder() {
        return requestedOrder(UUID.randomUUID(), UUID.randomUUID());
    }

    public static Order paidOrder(UUID buyerId, UUID sellerId) {
        Order o = requestedOrder(buyerId, sellerId);
        o.requestPayment();
        o.markPaid();
        return o;
    }

    public static Order shippingOrder(UUID buyerId, UUID sellerId) {
        Order o = paidOrder(buyerId, sellerId);
        o.startShipping();
        return o;
    }

    public static Order returnRequestedOrder(UUID buyerId, UUID sellerId) {
        Order o = shippingOrder(buyerId, sellerId);
        o.requestReturn(Reason.of("불량"));
        return o;
    }
}
