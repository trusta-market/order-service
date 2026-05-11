package com.trustamarket.orderservice.order.domain.model;

public enum OrderStatus {

    REQUESTED,                  // 주문 생성됨
    PAYMENT_PENDING,            // 결제 시작 (Wallet 차감 대기)
    PAID,                       // 결제 완료 (Wallet escrow 보관)
    SHIPPING,                   // 배송 중
    DELIVERED,                  // 배송 완료
    CONFIRMED,                  // 구매 확정 — 거래 종결 (정산 트리거)
    CANCELLED,                  // 결제 전 취소 (wallet 차감 안 됨)
    CANCELLATION_PROCESSING,    // 결제 후 취소 — wallet escrow 복구 대기
    CANCELLATION_COMPLETED,     // 결제 후 취소 완료 — escrow 복구 끝
    RETURN_REQUESTED,           // 반송 요청 (배송 시작 후만 가능)
    RETURN_APPROVED,            // 반송 승인
    RETURN_REJECTED;            // 반송 거절

    // 종결 상태 — 더 이상 OrderTransition 표에 out-going 전이 없음
    public boolean isTerminal() {
        return this == CONFIRMED
                || this == CANCELLED
                || this == CANCELLATION_COMPLETED
                || this == RETURN_REJECTED
                || this == RETURN_APPROVED;
    }
}
