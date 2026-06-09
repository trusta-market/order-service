package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.model.PaymentReconciliation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// PaymentReconciliation 영속화 port (out).
public interface PaymentReconciliationRepository {

    // 새 PENDING row 등록. 같은 orderId 가 이미 있으면 INSERT 안 함 (조용히 skip).
    // saga catch 안에서 호출 — 동시 실패 케이스에서 중복 등록 방지.
    void enqueueIfAbsent(PaymentReconciliation reconciliation);

    // 폴링 — 한 번에 가져올 row 수 제한.
    List<PaymentReconciliation> findRetriable(Instant now, int limit);

    // 도메인 상태 변경 후 저장.
    PaymentReconciliation save(PaymentReconciliation reconciliation);
}
