package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.model.Order;

// 주문 확정 직후 product 도메인에 판매완료 신호 (Kafka). product-service 가 consume.
public interface ProductSoldOutMessagePort {
    void publishForConfirmedOrder(Order order);
}
