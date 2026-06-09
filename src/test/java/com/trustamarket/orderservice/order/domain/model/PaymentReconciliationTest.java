package com.trustamarket.orderservice.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReconciliationTest {

    @Test
    @DisplayName("enqueue — PENDING, retry_count=0, next_retry_at=NOW+30s")
    void enqueue() {
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.parse("2026-06-03T00:00:00Z");

        PaymentReconciliation r = PaymentReconciliation.enqueue(orderId, now);

        assertThat(r.getOrderId()).isEqualTo(orderId);
        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.PENDING);
        assertThat(r.getRetryCount()).isZero();
        assertThat(r.getNextRetryAt()).isEqualTo(now.plus(Duration.ofSeconds(30)));
        assertThat(r.getLastAttemptAt()).isNull();
        assertThat(r.getLastError()).isNull();
    }

    @Test
    @DisplayName("markDone — DONE 으로 전이 + lastAttemptAt 갱신")
    void markDone() {
        PaymentReconciliation r = PaymentReconciliation.enqueue(UUID.randomUUID(), Instant.now());
        Instant t = Instant.parse("2026-06-03T00:01:00Z");

        r.markDone(t);

        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.DONE);
        assertThat(r.getLastAttemptAt()).isEqualTo(t);
    }

    @Test
    @DisplayName("recordUnknown 1회 — retry_count=1, next_retry_at=NOW+60s (BACKOFFS[1])")
    void recordUnknown_first() {
        PaymentReconciliation r = PaymentReconciliation.enqueue(UUID.randomUUID(), Instant.now());
        Instant t = Instant.parse("2026-06-03T00:01:00Z");

        r.recordUnknown("timeout", t);

        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.PENDING);
        assertThat(r.getRetryCount()).isEqualTo(1);
        assertThat(r.getNextRetryAt()).isEqualTo(t.plus(Duration.ofSeconds(60)));
        assertThat(r.getLastAttemptAt()).isEqualTo(t);
        assertThat(r.getLastError()).isEqualTo("timeout");
    }

    @Test
    @DisplayName("recordUnknown 2회 — retry_count=2, next_retry_at=NOW+120s (BACKOFFS[2])")
    void recordUnknown_second() {
        PaymentReconciliation r = PaymentReconciliation.enqueue(UUID.randomUUID(), Instant.now());
        r.recordUnknown("e1", Instant.parse("2026-06-03T00:01:00Z"));
        Instant t = Instant.parse("2026-06-03T00:02:00Z");

        r.recordUnknown("e2", t);

        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.PENDING);
        assertThat(r.getRetryCount()).isEqualTo(2);
        assertThat(r.getNextRetryAt()).isEqualTo(t.plus(Duration.ofSeconds(120)));
    }

    @Test
    @DisplayName("recordUnknown 3회 도달 — GIVEN_UP 전이, next_retry_at 갱신 X")
    void recordUnknown_givenUp() {
        PaymentReconciliation r = PaymentReconciliation.enqueue(UUID.randomUUID(), Instant.now());
        Instant beforeGivenUp = r.getNextRetryAt();
        r.recordUnknown("e1", Instant.parse("2026-06-03T00:01:00Z"));
        r.recordUnknown("e2", Instant.parse("2026-06-03T00:02:00Z"));
        Instant nextRetryBefore = r.getNextRetryAt();

        r.recordUnknown("e3", Instant.parse("2026-06-03T00:03:00Z"));

        assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.GIVEN_UP);
        assertThat(r.getRetryCount()).isEqualTo(PaymentReconciliation.MAX_RETRIES);
        assertThat(r.getLastError()).isEqualTo("e3");
        // GIVEN_UP 이후 next_retry_at 변경 X (스케줄러가 picking 안 함)
        assertThat(r.getNextRetryAt()).isEqualTo(nextRetryBefore);
        // 시작 시점의 next_retry_at 도 동일 (compile/sanity)
        assertThat(beforeGivenUp).isNotNull();
    }
}
