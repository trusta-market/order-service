package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderStatusHistoryJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

// Spring Data Repository — package-private (어댑터 안에서만)
// JpaRepository 대신 marker Repository 상속 → save/find 메서드만 노출
// p_order_status_history는 append-only — UPDATE/DELETE API를 컴파일 타임에 차단
interface OrderStatusHistoryJpaRepository extends Repository<OrderStatusHistoryJpaEntity, UUID> {

    OrderStatusHistoryJpaEntity save(OrderStatusHistoryJpaEntity entity);

    List<OrderStatusHistoryJpaEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
