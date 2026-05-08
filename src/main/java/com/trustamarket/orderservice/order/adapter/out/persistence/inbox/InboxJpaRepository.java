package com.trustamarket.orderservice.order.adapter.out.persistence.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// 외부 dedup 은 (idempotency_key, purpose) composite — 다른 흐름에서 같은 키가 재사용돼도 충돌 X.
// Kafka dedup 은 (event_id, consumer_group) — 동일 메시지의 같은 그룹 중복 차단.
public interface InboxJpaRepository extends JpaRepository<InboxJpaEntity, UUID> {

    Optional<InboxJpaEntity> findByIdempotencyKeyAndPurpose(String idempotencyKey, InboxPurpose purpose);

    Optional<InboxJpaEntity> findByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
