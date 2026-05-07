package com.trustamarket.orderservice.order.adapter.out.messaging.outbox;

// outbox row 상태. PENDING → PUBLISHED (정상) 또는 PENDING → FAILED (5회 retry 후).
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
