package com.trustamarket.orderservice.order.adapter.out.persistence.inbox;

import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

// InboxRepository (application port) 의 JPA 구현체.
// PurposeKey ↔ InboxPurpose 변환은 본 어댑터 안에서 처리 — application 이 JPA enum 에 직접 의존하지 않음.
@Component
@RequiredArgsConstructor
public class InboxJpaAdapter implements InboxRepository {

    private final InboxJpaRepository inboxJpaRepository;

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey, InboxPurposeKey purpose) {
        return inboxJpaRepository.findByIdempotencyKeyAndPurpose(idempotencyKey, toJpaPurpose(purpose)).isPresent();
    }

    @Override
    public boolean existsByKafkaEvent(UUID eventId, String consumerGroup) {
        return inboxJpaRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup).isPresent();
    }

    @Override
    public void recordIdempotencyKey(String idempotencyKey, InboxPurposeKey purpose) {
        inboxJpaRepository.save(InboxJpaEntity.forIdempotencyKey(idempotencyKey, toJpaPurpose(purpose), null));
    }

    @Override
    public void recordKafkaEvent(UUID eventId, String consumerGroup, InboxPurposeKey purpose) {
        inboxJpaRepository.save(InboxJpaEntity.forKafkaEvent(eventId, consumerGroup, toJpaPurpose(purpose)));
    }

    private static InboxPurpose toJpaPurpose(InboxPurposeKey key) {
        return switch (key) {
            case REQUEST_PAYMENT     -> InboxPurpose.REQUEST_PAYMENT;
            case DELIVERY_STARTED    -> InboxPurpose.DELIVERY_STARTED;
            case DELIVERY_COMPLETED  -> InboxPurpose.DELIVERY_COMPLETED;
        };
    }
}
