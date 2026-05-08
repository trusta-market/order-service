package com.trustamarket.orderservice.order.adapter.out.persistence.inbox;

import com.trustamarket.common.domain.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

// p_order_inbox — 외부 호출 / Kafka 메시지의 멱등성 처리용.
// 두 가지 사용처:
//   1) 외부 호출: idempotency_key UNIQUE — 동일 키 재시도 시 기존 결과 반환
//   2) Kafka 메시지: (event_id, consumer_group) UNIQUE — 동일 메시지 재배달 차단
// purpose 가 흐름을 구분.
@Entity
@Table(name = "p_order_inbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxJpaEntity extends BaseCreatedEntity {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "event_id", columnDefinition = "uuid", updatable = false)
    private UUID eventId;

    @Column(name = "consumer_group", length = 50, updatable = false)
    private String consumerGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30, updatable = false)
    private InboxPurpose purpose;

    // 동일 키 재시도 시 반환할 결과 (HTTP 응답 등). nullable.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_snapshot", columnDefinition = "jsonb", updatable = false)
    private String resultSnapshot;

    // private 생성자 — 정적 팩토리만 통해서 entity 생성 가능. Builder 우회 차단.
    private InboxJpaEntity(UUID id, String idempotencyKey, UUID eventId,
                           String consumerGroup, InboxPurpose purpose, String resultSnapshot) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.purpose = purpose;
        this.resultSnapshot = resultSnapshot;
    }

    // 외부 호출 멱등성 record 생성 (POST /payments 의 Idempotency-Key 흐름).
    public static InboxJpaEntity forIdempotencyKey(String idempotencyKey, InboxPurpose purpose,
                                                   String resultSnapshot) {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        return new InboxJpaEntity(UUID.randomUUID(), idempotencyKey, null, null, purpose, resultSnapshot);
    }

    // Kafka 메시지 멱등성 record 생성 (delivery consumer 흐름).
    public static InboxJpaEntity forKafkaEvent(UUID eventId, String consumerGroup, InboxPurpose purpose) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        if (consumerGroup.isBlank()) {
            throw new IllegalArgumentException("consumerGroup must not be blank");
        }
        return new InboxJpaEntity(UUID.randomUUID(), null, eventId, consumerGroup, purpose, null);
    }
}
