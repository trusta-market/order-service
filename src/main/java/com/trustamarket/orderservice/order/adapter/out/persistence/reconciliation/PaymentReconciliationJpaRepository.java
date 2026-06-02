package com.trustamarket.orderservice.order.adapter.out.persistence.reconciliation;

import com.trustamarket.orderservice.order.domain.model.ReconciliationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Spring Data JPA — package-private 으로 외부 노출 X. Adapter 가 단일 진입점.
interface PaymentReconciliationJpaRepository extends JpaRepository<PaymentReconciliationJpaEntity, UUID> {

    // polling 쿼리 — (status, next_retry_at) 인덱스 활용.
    List<PaymentReconciliationJpaEntity> findByStatusAndNextRetryAtLessThanEqual(
            ReconciliationStatus status, Instant now, Pageable pageable);

    boolean existsByOrderId(UUID orderId);
}
