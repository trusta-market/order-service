package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.domain.exception.OrderNotFoundException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// OrderRepository port의 JPA 구현 — application 레이어가 의존하는 단일 진입점
// Spring Data Repository는 package-private(OrderJpaRepository) → 외부 노출 X
@Repository
@RequiredArgsConstructor
public class OrderJpaRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = mapper.toEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Order findByIdOrThrow(OrderId id) {
        return findById(id).orElseThrow(() -> new OrderNotFoundException(id.value()));
    }
}
