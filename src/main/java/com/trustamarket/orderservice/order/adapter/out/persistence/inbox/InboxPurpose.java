package com.trustamarket.orderservice.order.adapter.out.persistence.inbox;

// inbox row 가 어떤 흐름의 멱등성 기록인지.
public enum InboxPurpose {
    REQUEST_PAYMENT,        // POST /payments 외부 호출 멱등성 (Idempotency-Key)
    DELIVERY_STARTED,       // delivery.started Kafka 메시지 멱등성
    DELIVERY_COMPLETED      // delivery.completed Kafka 메시지 멱등성
}
