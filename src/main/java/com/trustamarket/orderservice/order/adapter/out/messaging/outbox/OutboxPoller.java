package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

// outbox 테이블의 PENDING 행을 fetch → Kafka publish → PUBLISHED 마킹.
// fixedDelay = 1초 (이전 실행 종료 후 1초 대기, race 없음).
// LIMIT 100 — 한 tick batch.
// FOR UPDATE SKIP LOCKED 로 멀티 인스턴스 안전.
//
// 발행 실패 시 retry_count++ + last_error. 5회 도달 시 FAILED 로 종결 (운영 알림은 후속).
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_LIMIT = 100;
    private static final int MAX_RETRIES = 5;
    private static final long PUBLISH_TIMEOUT_SECONDS = 5;

    private final OutboxJpaRepository outboxRepository;
    // outbox.payload 가 이미 JSON 문자열이라 StringSerializer 매칭.
    private final KafkaTemplate<String, String> kafkaTemplate;

    // @Transactional — fetchPending 의 SELECT FOR UPDATE 락이 트랜잭션 종료까지 유지되어야 함.
    // 한 batch 안에서 모든 행 처리 후 commit.
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        List<OutboxJpaEntity> rows = outboxRepository.fetchPending(BATCH_LIMIT);
        if (rows.isEmpty()) {
            return;
        }
        log.info("[Outbox] poll — {} 건", rows.size());

        for (OutboxJpaEntity row : rows) {
            try {
                kafkaTemplate.send(row.getTopic(), row.getDomainId().toString(), row.getPayload())
                        .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                row.markPublished();
                log.info("[Outbox] published — correlationId={}, eventType={}, topic={}",
                        row.getCorrelationId(), row.getEventType(), row.getTopic());
            } catch (Exception e) {
                row.recordFailure(e.getClass().getSimpleName() + ": " + e.getMessage(), MAX_RETRIES);
                log.error("[Outbox] publish 실패 — correlationId={}, eventType={}, retry={}/{}",
                        row.getCorrelationId(), row.getEventType(),
                        row.getRetryCount(), MAX_RETRIES, e);
                // 다른 row 는 계속 진행 (한 메시지 실패가 batch 전체를 막지 않음)
            }
        }
    }
}
