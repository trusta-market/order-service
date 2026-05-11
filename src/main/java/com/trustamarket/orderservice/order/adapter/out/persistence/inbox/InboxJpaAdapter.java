package com.trustamarket.orderservice.order.adapter.out.persistence.inbox;

import com.trustamarket.orderservice.order.application.port.out.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// InboxRepository 의 JPA 구현체.
// 멱등성 처리는 atomic INSERT — UNIQUE 제약 충돌 (DataIntegrityViolationException) 을 catch 해서 false 반환.
//   exists + record 분리 패턴 (TOCTOU race) 회피.
//
// REQUIRES_NEW 트랜잭션: 동시 INSERT 충돌 시 부모 트랜잭션을 mark-as-rollback 시키지 않게 분리.
@Slf4j
@Component
@RequiredArgsConstructor
public class InboxJpaAdapter implements InboxRepository {

    private final InboxJpaRepository inboxJpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryRecordIdempotencyKey(String idempotencyKey, InboxPurposeKey purpose) {
        try {
            inboxJpaRepository.saveAndFlush(
                    InboxJpaEntity.forIdempotencyKey(idempotencyKey, toJpaPurpose(purpose), null));
            return true;
        } catch (DataIntegrityViolationException e) {
            // (idempotency_key, purpose) UNIQUE 충돌 — 이미 처리됨.
            log.debug("[Inbox] 중복 Idempotency-Key — purpose={}, key={}", purpose, idempotencyKey);
            return false;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryRecordKafkaEvent(UUID eventId, String consumerGroup, InboxPurposeKey purpose) {
        try {
            inboxJpaRepository.saveAndFlush(
                    InboxJpaEntity.forKafkaEvent(eventId, consumerGroup, toJpaPurpose(purpose)));
            return true;
        } catch (DataIntegrityViolationException e) {
            // (event_id, consumer_group) UNIQUE 충돌 — 동일 메시지 중복 배달.
            log.debug("[Inbox] 중복 Kafka eventId — consumerGroup={}, eventId={}", consumerGroup, eventId);
            return false;
        }
    }

    private static InboxPurpose toJpaPurpose(InboxPurposeKey key) {
        return switch (key) {
            case REQUEST_PAYMENT                -> InboxPurpose.REQUEST_PAYMENT;
            case DELIVERY_STARTED               -> InboxPurpose.DELIVERY_STARTED;
            case DELIVERY_COMPLETED             -> InboxPurpose.DELIVERY_COMPLETED;
            case WALLET_CANCELLATION_COMPLETED  -> InboxPurpose.WALLET_CANCELLATION_COMPLETED;
        };
    }
}
