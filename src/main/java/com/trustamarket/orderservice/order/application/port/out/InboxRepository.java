package com.trustamarket.orderservice.order.application.port.out;

import java.util.UUID;

// 멱등성 처리 port — application 레이어가 persistence 구현 (Inbox JPA 등) 에 직접 의존하지 않게 추상화.
//
// **TOCTOU 회피**: exists + record 분리 패턴은 동시성에 약함. 단일 atomic 호출 (`tryRecordXxx`) 로 통일.
//   어댑터에서 INSERT 시 UNIQUE 제약 충돌이 나면 false (이미 존재) 반환, 새 INSERT 면 true.
//
// 두 가지 흐름:
//   1) 외부 호출 (Idempotency-Key 헤더): (idempotency_key, purpose) UNIQUE 로 dedup
//   2) Kafka 메시지 (consumer 멱등성): (event_id, consumer_group) UNIQUE 로 dedup
public interface InboxRepository {

    // 외부 호출 멱등성 — 동일 (idempotencyKey, purpose) 가 처음이면 true (INSERT 됨), 이미 있으면 false.
    boolean tryRecordIdempotencyKey(String idempotencyKey, InboxPurposeKey purpose);

    // Kafka 메시지 멱등성 — 동일 (eventId, consumerGroup) 가 처음이면 true, 이미 있으면 false.
    boolean tryRecordKafkaEvent(UUID eventId, String consumerGroup, InboxPurposeKey purpose);

    // application 이 persistence 의 InboxPurpose enum 에 직접 의존하지 않게 별도 enum.
    enum InboxPurposeKey {
        REQUEST_PAYMENT,
        DELIVERY_STARTED,
        DELIVERY_COMPLETED,
        WALLET_CANCELLATION_COMPLETED
    }
}
