package com.trustamarket.orderservice.order.adapter.out.persistence.reconciliation;

import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import org.springframework.stereotype.Component;

// 도메인 ↔ JPA Entity 양방향 변환.
// audit 필드 (createdAt/updatedAt/createdBy/updatedBy/deletedAt/deletedBy) 는 BaseUserEntity 가 JPA Auditing 으로 자동 채움.
// Mapper 는 도메인 식별/상태/재시도 메타만 다룸.
@Component
public class PaymentReconciliationMapper {

    public PaymentReconciliationJpaEntity toEntity(PaymentReconciliation domain) {
        return PaymentReconciliationJpaEntity.builder()
                .id(domain.getId())
                .orderId(domain.getOrderId())
                .status(domain.getStatus())
                .retryCount(domain.getRetryCount())
                .nextRetryAt(domain.getNextRetryAt())
                .lastAttemptAt(domain.getLastAttemptAt())
                .lastError(domain.getLastError())
                .build();
    }

    public PaymentReconciliation toDomain(PaymentReconciliationJpaEntity entity) {
        return PaymentReconciliation.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .nextRetryAt(entity.getNextRetryAt())
                .lastAttemptAt(entity.getLastAttemptAt())
                .lastError(entity.getLastError())
                .build();
    }
}
