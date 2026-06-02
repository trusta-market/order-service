package com.trustamarket.orderservice.order.domain.model;

// PaymentReconciliation 의 상태.
// PENDING:   재시도 대기. polling 이 next_retry_at <= NOW() 조건으로 fetch.
// DONE:      DEDUCTED 또는 NOT_DEDUCTED 결과를 받아 처리 완료. 추가 폴링 X.
// GIVEN_UP:  retry_count 가 MAX_RETRIES (3) 도달. 운영자 (Discord webhook) 알림 후 수동 처리.
public enum ReconciliationStatus {
    PENDING,
    DONE,
    GIVEN_UP
}
