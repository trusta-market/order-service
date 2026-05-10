package com.trustamarket.orderservice.order.adapter.out.persistence.outbox;

import com.trustamarket.common.domain.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

// p_order_outbox — 발행 대기 중인 메시지 큐.
// 상태 전이: PENDING → IN_PROGRESS → PUBLISHED (또는 FAILED)
//   - PENDING: listener 가 INSERT 직후
//   - IN_PROGRESS: poller 가 claim (락 보유 시간 최소화 — Kafka publish 는 트랜잭션 밖)
//   - PUBLISHED: publish 성공 + 마킹 완료
//   - FAILED: retry 5회 도달 (운영 알림은 후속 작업)
// payload 는 jsonb — Jackson 으로 직렬화된 메시지 본문.
@Entity
@Table(name = "p_order_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxJpaEntity extends BaseCreatedEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 36)
    private String correlationId;

    @Column(name = "domain_type", nullable = false, updatable = false, length = 30)
    private String domainType;

    @Column(name = "domain_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID domainId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(name = "topic", nullable = false, updatable = false, length = 100)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Builder
    private OutboxJpaEntity(UUID id, String correlationId, String domainType, UUID domainId,
                            String eventType, String topic, String payload) {
        this.id = id;
        this.correlationId = correlationId;
        this.domainType = domainType;
        this.domainId = domainId;
        this.eventType = eventType;
        this.topic = topic;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    // claim 단계 — poller 가 PENDING 행을 fetch 직후 호출.
    // IN_PROGRESS 마킹과 동시에 트랜잭션 commit → DB 락 해제 → Kafka publish 는 트랜잭션 밖에서 진행.
    public void markInProgress() {
        this.status = OutboxStatus.IN_PROGRESS;
    }

    // Kafka publish 성공 시 호출 (별도 트랜잭션).
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    // Kafka publish 실패 시 호출. retry 5회 도달 시 FAILED.
    // FAILED 로 가지 않으면 PENDING 으로 되돌려서 다음 polling 에서 재시도.
    public void recordFailure(String error, int maxRetries) {
        this.retryCount += 1;
        this.lastError = error;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.status = OutboxStatus.PENDING;   // 재시도 위해 다시 PENDING
        }
    }
}
