package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;

import java.util.Optional;

// Order 영속화 port (out)
// 어댑터(adapter.out.persistence)가 구현. application/도메인은 이 인터페이스만 의존
// 메서드는 use case 정의되면서 추가 — 현재는 최소만 (PR 5에서 확장)
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);

    Order findByIdOrThrow(OrderId id);
}
