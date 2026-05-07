package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

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
// listener 가 INSERT (PENDING), poller 가 PUBLISHED 마킹.
// payload 는 jsonb — Jackson 으로 직렬화된 메시지 본문.
// createdAt 은 BaseCreatedEntity 가 자동 채움. published_at 은 마킹 시점으로 자체 관리.
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

    // jsonb — Jackson 직렬화된 메시지 본문 (record JSON 그대로).
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

    // poller 가 발행 성공 시 호출.
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    // poller 가 발행 실패 시 호출. retry 5회 도달 시 FAILED 로 종결.
    public void recordFailure(String error, int maxRetries) {
        this.retryCount += 1;
        this.lastError = error;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
