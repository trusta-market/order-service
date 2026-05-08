package com.trustamarket.orderservice.order.adapter.out.persistence.outbox;

// outbox row 상태.
//   PENDING     → claim 단계 (poller 가 fetch + IN_PROGRESS 마킹)
//   IN_PROGRESS → Kafka publish 시도 중 (트랜잭션 밖에서 publish, 락 해제됨)
//   PUBLISHED   → publish 성공 + 마킹 완료
//   FAILED      → retry 5회 도달, 운영 알림 후속
public enum OutboxStatus {
    PENDING,
    IN_PROGRESS,
    PUBLISHED,
    FAILED
}
