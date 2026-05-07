package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// PostgreSQL native lock 사용 — `FOR UPDATE SKIP LOCKED` 로 멀티 인스턴스 환경에서도 동일 row 중복 발행 차단.
// native query 는 @Lock 어노테이션 못 받음 (Hibernate 가 native 에 lockMode 막음). SQL 안의 FOR UPDATE 로만 잠금 표현.
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    // status='PENDING' 인 행을 오래된 순으로 fetch + 행 잠금 + 다른 트랜잭션이 잠근 행은 스킵.
    @Query(value = """
            SELECT * FROM p_order_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxJpaEntity> fetchPending(@Param("limit") int limit);
}
