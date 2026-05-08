package com.trustamarket.orderservice.order.application.port.out;

import java.util.UUID;

// 멱등성 처리 port — application 레이어가 persistence 구현 (Inbox JPA 등) 에 직접 의존하지 않게 추상화.
// 두 가지 흐름:
//   1) 외부 호출 (Idempotency-Key 헤더): purpose + idempotency_key 로 dedup
//   2) Kafka 메시지 (consumer 멱등성): eventId + consumerGroup 으로 dedup
public interface InboxRepository {

    // 외부 호출 멱등성 — 동일 (idempotencyKey, purpose) 가 이미 있으면 true.
    boolean existsByIdempotencyKey(String idempotencyKey, InboxPurposeKey purpose);

    // Kafka 메시지 멱등성 — 동일 (eventId, consumerGroup) 가 이미 있으면 true.
    boolean existsByKafkaEvent(UUID eventId, String consumerGroup);

    // 외부 호출 처리 완료 기록 (멱등성 키 보존).
    void recordIdempotencyKey(String idempotencyKey, InboxPurposeKey purpose);

    // Kafka 메시지 처리 완료 기록.
    void recordKafkaEvent(UUID eventId, String consumerGroup, InboxPurposeKey purpose);

    // application 이 persistence 의 InboxPurpose enum 에 직접 의존하지 않게 별도 enum.
    // 값은 InboxPurpose 와 1:1 매핑. 어댑터에서 변환.
    enum InboxPurposeKey {
        REQUEST_PAYMENT,
        DELIVERY_STARTED,
        DELIVERY_COMPLETED
    }
}
