package com.trustamarket.orderservice.order.adapter.out.persistence.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InboxJpaRepository extends JpaRepository<InboxJpaEntity, UUID> {

    Optional<InboxJpaEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<InboxJpaEntity> findByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
