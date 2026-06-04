package com.trustamarket.orderservice.order.adapter.out.persistence.reconciliation;

import com.trustamarket.orderservice.order.application.port.out.PaymentReconciliationRepository;
import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;
import com.trustamarket.orderservice.order.domain.model.ReconciliationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

// PaymentReconciliationRepository port 의 JPA 구현 — application 진입점.
@Repository
@RequiredArgsConstructor
public class PaymentReconciliationJpaRepositoryAdapter implements PaymentReconciliationRepository {

    private final PaymentReconciliationJpaRepository jpaRepository;
    private final PaymentReconciliationMapper mapper;

    // saga catch 안에서 호출. existsByOrderId 로 중복 INSERT 회피 + UNIQUE(order_id) 가 동시성 안전망.
    // TOCTOU race (exists check 통과 후 다른 트랜잭션이 먼저 INSERT) 시 DataIntegrityViolationException 발생 →
    // 멱등 skip (어차피 같은 orderId 의 PENDING row 가 이미 존재).
    @Override
    public void enqueueIfAbsent(PaymentReconciliation reconciliation) {
        if (jpaRepository.existsByOrderId(reconciliation.getOrderId())) {
            return;
        }
        try {
            jpaRepository.save(mapper.toEntity(reconciliation));
        } catch (DataIntegrityViolationException e) {
            // UNIQUE(order_id) 가 잡은 동시 실패 race — 다른 트랜잭션이 이미 등록했으므로 멱등 skip.
        }
    }

    // 결정적 정렬 — nextRetryAt 오름차순으로 가장 오래 대기한 row 부터 처리.
    // 배치 한도 (50) 초과 백로그 시 특정 row 가 계속 뒤로 밀려 starvation 되는 것 방지.
    @Override
    public List<PaymentReconciliation> findRetriable(Instant now, int limit) {
        return jpaRepository.findByStatusAndNextRetryAtLessThanEqual(
                ReconciliationStatus.PENDING, now,
                PageRequest.of(0, limit, Sort.by("nextRetryAt").ascending())
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
