package com.trustamarket.orderservice.order.adapter.out.persistence;

import com.trustamarket.orderservice.order.adapter.out.persistence.jpa.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

// Spring Data JPA Repository — package-private, 어댑터 안에서만 사용
// 외부(application/도메인)는 OrderRepository port만 봐야 함
// JpaSpecificationExecutor — search() 동적 조건 쿼리에 사용
interface OrderJpaRepository extends
        JpaRepository<OrderJpaEntity, UUID>,
        JpaSpecificationExecutor<OrderJpaEntity> {

    Page<OrderJpaEntity> findByBuyerId(UUID buyerId, Pageable pageable);

    Page<OrderJpaEntity> findBySellerId(UUID sellerId, Pageable pageable);

    Page<OrderJpaEntity> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId, Pageable pageable);
}
