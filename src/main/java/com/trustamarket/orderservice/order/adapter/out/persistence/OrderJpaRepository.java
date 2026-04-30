package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Spring Data JPA Repository — package-private, 어댑터 안에서만 사용
// 외부(application/도메인)는 OrderRepository port만 봐야 함
interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
