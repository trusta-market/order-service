package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// outbox PENDING → IN_PROGRESS → PUBLISHED (또는 FAILED) 흐름을 3단계 트랜잭션으로 분리:
//   1. claim   (Tx1, 짧음): SELECT ... FOR UPDATE SKIP LOCKED + IN_PROGRESS 마킹 → 즉시 commit (락 해제)
//   2. publish (트랜잭션 밖): KafkaTemplate.send(...).get() — DB 락 보유 X, 커넥션 풀 안전
//   3. finalize (Tx3, 짧음): publish 결과로 PUBLISHED / FAILED 마킹
//
// 핵심: SELECT FOR UPDATE 락이 Kafka 응답을 기다리는 동안 유지되지 않게 하여 DB 커넥션 풀 고갈 방지.
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_LIMIT = 100;
    private static final int MAX_RETRIES = 5;
    private static final long PUBLISH_TIMEOUT_SECONDS = 5;

    private final OutboxJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxJpaEntity> claimed = claimPending();
        if (claimed.isEmpty()) return;

        log.info("[Outbox] poll — claimed {} 건", claimed.size());

        Map<UUID, Throwable> results = new HashMap<>();
        for (OutboxJpaEntity row : claimed) {
            try {
                kafkaTemplate.send(row.getTopic(), row.getDomainId().toString(), row.getPayload())
                        .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                results.put(row.getId(), null);
            } catch (Exception e) {
                results.put(row.getId(), e);
            }
        }

        finalizeRows(results);
    }

    // Tx1 — claim. 락 보유 시간 = SELECT + UPDATE 만 (수 ms).
    @Transactional
    public List<OutboxJpaEntity> claimPending() {
        List<OutboxJpaEntity> rows = outboxRepository.fetchPending(BATCH_LIMIT);
        if (rows.isEmpty()) return List.of();
        // detached snapshot 필요 — Tx1 commit 후에도 row 정보는 사용해야 하니 ID 유지를 위한 별도 필드 캐싱.
        List<OutboxJpaEntity> claimed = new ArrayList<>(rows.size());
        for (OutboxJpaEntity row : rows) {
            row.markInProgress();
            claimed.add(row);
        }
        return claimed;
    }

    // Tx3 — finalize. 별도 트랜잭션으로 Kafka 응답 기준 PUBLISHED / FAILED 마킹.
    @Transactional
    public void finalizeRows(Map<UUID, Throwable> results) {
        for (Map.Entry<UUID, Throwable> entry : results.entrySet()) {
            outboxRepository.findById(entry.getKey()).ifPresent(row -> {
                Throwable ex = entry.getValue();
                if (ex == null) {
                    row.markPublished();
                    log.info("[Outbox] published — correlationId={}, eventType={}, topic={}",
                            row.getCorrelationId(), row.getEventType(), row.getTopic());
                } else {
                    String error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    row.recordFailure(error, MAX_RETRIES);
                    log.error("[Outbox] publish 실패 — correlationId={}, eventType={}, retry={}/{}",
                            row.getCorrelationId(), row.getEventType(),
                            row.getRetryCount(), MAX_RETRIES, ex);
                }
            });
        }
    }
}
