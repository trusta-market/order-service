package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

// publish 결과로 PUBLISHED / FAILED 마킹 — 별도 빈으로 분리해서 self-invocation 회피.
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxFinalizer {

    private final OutboxJpaRepository outboxRepository;

    @Transactional
    public void finalizeRows(Map<UUID, Throwable> results, int maxRetries) {
        for (Map.Entry<UUID, Throwable> entry : results.entrySet()) {
            outboxRepository.findById(entry.getKey()).ifPresent(row -> {
                Throwable ex = entry.getValue();
                if (ex == null) {
                    row.markPublished();
                    log.info("[Outbox] published — correlationId={}, eventType={}, topic={}",
                            row.getCorrelationId(), row.getEventType(), row.getTopic());
                } else {
                    String error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    row.recordFailure(error, maxRetries);
                    log.error("[Outbox] publish 실패 — correlationId={}, eventType={}, retry={}/{}",
                            row.getCorrelationId(), row.getEventType(),
                            row.getRetryCount(), maxRetries, ex);
                }
            });
        }
    }
}
