package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderStatusHistoryJpaEntity;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import com.trustamarket.orderservice.order.domain.model.Reason;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusHistoryMapper {

    public OrderStatusHistoryJpaEntity toEntity(OrderStatusHistory history) {
        return OrderStatusHistoryJpaEntity.builder()
                .id(history.id())
                .orderId(history.orderId().value())
                .prevStatus(history.prevStatus())
                .nextStatus(history.nextStatus())
                .reason(history.reason() == null ? null : history.reason().value())
                .build();
    }

    public OrderStatusHistory toDomain(OrderStatusHistoryJpaEntity entity) {
        return OrderStatusHistory.restore(
                entity.getId(),
                OrderId.of(entity.getOrderId()),
                entity.getPrevStatus(),
                entity.getNextStatus(),
                entity.getReason() == null ? null : Reason.of(entity.getReason())
        );
    }
}
