package com.trustamarket.orderservice.order.domain.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// p_payment_reconciliation 의 도메인 모델.
// 결제 saga 의 wallet Feign 호출이 timeout / 5xx 등 비정상 응답을 받은 후 즉시 결과 확인 (getUsage) 까지 실패했을 때
// enqueue 되는 재처리 row.
//
// 상태 전이:
//   PENDING ─ getUsage 가 DEDUCTED / INSUFFICIENT / NOT_FOUND 중 하나로 응답 ─→ DONE
//   PENDING ─ getUsage 가 또 실패 ─→ retry_count++, next_retry_at = NOW + BACKOFFS[retry_count]
//   retry_count >= MAX_RETRIES (3) ─→ GIVEN_UP (운영자 알림)
//
// 백오프: retry_count=1 일 때 BACKOFFS[1]=60s, retry_count=2 일 때 BACKOFFS[2]=120s.
// (retry_count=0 → enqueue 시점에 BACKOFFS[0]=30s 적용)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentReconciliation {

    public static final int MAX_RETRIES = 3;
    private static final List<Duration> BACKOFFS = List.of(
            Duration.ofSeconds(30),
            Duration.ofSeconds(60),
            Duration.ofSeconds(120)
    );

    private UUID id;
    private UUID orderId;
    private ReconciliationStatus status;
    private int retryCount;
    private Instant nextRetryAt;
    private Instant lastAttemptAt;
    private String lastError;
    // JPA Optimistic Lock 의 version — entity 와 일관성 유지 (mapper 가 양방향 매핑).
    // 도메인 로직에서 직접 변경하지 않음. JPA 가 entity 의 @Version 으로 자동 증가.
    private long version;

    @Builder
    private PaymentReconciliation(UUID id, UUID orderId, ReconciliationStatus status,
                                  int retryCount, Instant nextRetryAt,
                                  Instant lastAttemptAt, String lastError, long version) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.lastAttemptAt = lastAttemptAt;
        this.lastError = lastError;
        this.version = version;
    }

    // saga catch 안에서 결과 확인까지 실패했을 때 enqueue.
    // 첫 폴링 시점: NOW + BACKOFFS[0] (30s).
    public static PaymentReconciliation enqueue(UUID orderId, Instant now) {
        return PaymentReconciliation.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(ReconciliationStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(now.plus(BACKOFFS.get(0)))
                .build();
    }

    // 결과 확인 성공 — DEDUCTED / INSUFFICIENT / NOT_FOUND 어떤 응답이든 처리 끝.
    // 어떤 결과로 종료됐는지는 호출자가 별도 분기 (markPaid / rollbackToRequested).
    public void markDone(Instant now) {
        this.status = ReconciliationStatus.DONE;
        this.lastAttemptAt = now;
    }

    // 결과 확인 자체가 실패 — 다음 시도까지 백오프.
    // retry_count 가 MAX_RETRIES 도달 시 GIVEN_UP (운영자 알림 대상).
    public void recordUnknown(String error, Instant now) {
        this.retryCount += 1;
        this.lastAttemptAt = now;
        this.lastError = error;
        if (this.retryCount >= MAX_RETRIES) {
            this.status = ReconciliationStatus.GIVEN_UP;
        } else {
            int idx = Math.min(this.retryCount, BACKOFFS.size() - 1);
            this.nextRetryAt = now.plus(BACKOFFS.get(idx));
        }
    }
}
