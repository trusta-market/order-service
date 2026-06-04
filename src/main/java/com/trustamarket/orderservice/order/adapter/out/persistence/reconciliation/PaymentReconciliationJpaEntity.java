package com.trustamarket.orderservice.order.adapter.out.persistence.reconciliation;

import com.trustamarket.common.domain.BaseUserEntity;
import com.trustamarket.orderservice.order.domain.model.ReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// p_payment_reconciliation 테이블 매핑.
// 도메인 PaymentReconciliation 과 1:1. Mapper 가 양방향 변환.
// BaseUserEntity 상속 — created_at/updated_at + created_by/updated_by + deleted_at/deleted_by 자동.
// 시스템 (스케줄러) 처리라 by 는 AuditorAware 가 system UUID 또는 null 로 채움.
@Entity
@Table(name = "p_payment_reconciliation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentReconciliationJpaEntity extends BaseUserEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", columnDefinition = "uuid", nullable = false, updatable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReconciliationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    // Optimistic Lock — Hibernate 가 UPDATE 시 자동 증가, 충돌 시 OptimisticLockException.
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Builder
    private PaymentReconciliationJpaEntity(UUID id, UUID orderId, ReconciliationStatus status,
                                           int retryCount, Instant nextRetryAt,
                                           Instant lastAttemptAt, String lastError) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastAttemptAt = lastAttemptAt;
        this.lastError = lastError;
    }
}
