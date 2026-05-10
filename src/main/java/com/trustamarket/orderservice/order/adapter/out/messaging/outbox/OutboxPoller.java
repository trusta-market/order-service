package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// outbox 발행 폴러 — 3단계 흐름:
//   1. claim   (OutboxClaimer.@Transactional): PENDING fetch + IN_PROGRESS 마킹 → 즉시 commit
//   2. publish (트랜잭션 밖): KafkaTemplate.send().get() — DB 락 보유 X
//   3. finalize (OutboxFinalizer.@Transactional): publish 결과 PUBLISHED / FAILED 마킹
//
// claim/finalize 를 별도 빈으로 분리 — self-invocation 시 Spring AOP proxy 우회되는 문제 회피.
//
// @EnableScheduling 은 common 의 EventConfig 가 제공.
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int BATCH_LIMIT = 100;
    private static final int MAX_RETRIES = 5;
    private static final long PUBLISH_TIMEOUT_SECONDS = 5;

    private final OutboxClaimer outboxClaimer;
    private final OutboxFinalizer outboxFinalizer;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxJpaEntity> claimed = outboxClaimer.claimPending(BATCH_LIMIT);
        if (claimed.isEmpty()) return;

        log.info("[Outbox] poll — claimed {} 건", claimed.size());

        Map<UUID, Throwable> results = new HashMap<>();
        for (OutboxJpaEntity row : claimed) {
            try {
                kafkaTemplate.send(row.getTopic(), row.getDomainId().toString(), row.getPayload())
                        .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                results.put(row.getId(), null);
            } catch (InterruptedException e) {
                // 스레드 인터럽트 플래그 복원 — 상위 (스케줄러) 가 적절히 처리하도록.
                Thread.currentThread().interrupt();
                results.put(row.getId(), e);
                break;   // 인터럽트 시 batch 중단 — 다음 tick 에 잔여 재시도
            } catch (ExecutionException | TimeoutException e) {
                results.put(row.getId(), e);
            }
        }

        outboxFinalizer.finalizeRows(results, MAX_RETRIES);
    }
}
