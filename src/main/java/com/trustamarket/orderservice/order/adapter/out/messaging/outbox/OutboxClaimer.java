package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaEntity;
import com.trustamarket.orderservice.order.adapter.out.persistence.outbox.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// PENDING 행 claim 단계 — 별도 빈으로 분리해서 self-invocation 회피.
//   OutboxPoller 가 본 빈 호출 시 Spring AOP proxy 거쳐 @Transactional 정상 적용.
//
// 트랜잭션 경계: SELECT FOR UPDATE SKIP LOCKED + IN_PROGRESS 마킹만 — 수 ms 안에 commit.
//   이후 Kafka publish 는 트랜잭션 밖에서 진행 → DB 락 해제됨.
@Component
@RequiredArgsConstructor
public class OutboxClaimer {

    private final OutboxJpaRepository outboxRepository;

    @Transactional
    public List<OutboxJpaEntity> claimPending(int batchLimit) {
        List<OutboxJpaEntity> rows = outboxRepository.fetchPending(batchLimit);
        if (rows.isEmpty()) return List.of();
        List<OutboxJpaEntity> claimed = new ArrayList<>(rows.size());
        for (OutboxJpaEntity row : rows) {
            row.markInProgress();
            claimed.add(row);
        }
        return claimed;
    }
}
