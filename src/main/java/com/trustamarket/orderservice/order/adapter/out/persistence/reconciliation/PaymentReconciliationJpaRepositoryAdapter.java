package com.trustamarket.orderservice.order.adapter.out.persistence.reconciliation;

import com.trustamarket.orderservice.order.application.port.out.PaymentReconciliationRepository;
import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import com.trustamarket.orderservice.order.domain.model.ReconciliationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

// PaymentReconciliationRepository port 의 JPA 구현 — application 진입점.
@Repository
@RequiredArgsConstructor
public class PaymentReconciliationJpaRepositoryAdapter implements PaymentReconciliationRepository {

    private final PaymentReconciliationJpaRepository jpaRepository;
    private final PaymentReconciliationMapper mapper;

    // saga catch 안에서 호출. existsByOrderId 로 중복 INSERT 회피.
    // 동시 실패 race 가 발생해도 UNIQUE (order_id) 제약이 안전망.
    @Override
    public void enqueueIfAbsent(PaymentReconciliation reconciliation) {
        if (jpaRepository.existsByOrderId(reconciliation.getOrderId())) {
            return;
        }
        jpaRepository.save(mapper.toEntity(reconciliation));
    }

    @Override
    public List<PaymentReconciliation> findRetriable(Instant now, int limit) {
        return jpaRepository.findByStatusAndNextRetryAtLessThanEqual(
                ReconciliationStatus.PENDING, now, PageRequest.of(0, limit)
        ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PaymentReconciliation save(PaymentReconciliation reconciliation) {
        PaymentReconciliationJpaEntity entity = mapper.toEntity(reconciliation);
        PaymentReconciliationJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
