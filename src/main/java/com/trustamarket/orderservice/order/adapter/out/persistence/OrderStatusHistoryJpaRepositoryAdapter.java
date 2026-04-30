package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderStatusHistoryJpaEntity;
import com.trustamarket.orderservice.order.application.port.out.OrderStatusHistoryRepository;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderStatusHistoryJpaRepositoryAdapter implements OrderStatusHistoryRepository {

    private final OrderStatusHistoryJpaRepository jpaRepository;
    private final OrderStatusHistoryMapper mapper;

    @Override
    public OrderStatusHistory save(OrderStatusHistory history) {
        OrderStatusHistoryJpaEntity entity = mapper.toEntity(history);
        OrderStatusHistoryJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<OrderStatusHistory> findByOrderId(OrderId orderId) {
        return jpaRepository.findByOrderIdOrderByCreatedAtAsc(orderId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
