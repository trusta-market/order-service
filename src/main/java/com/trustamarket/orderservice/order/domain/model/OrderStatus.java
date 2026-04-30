package com.trustamarket.orderservice.order.domain.model;

public enum OrderStatus {

    REQUESTED,             // 주문 생성됨
    PAYMENT_PENDING,       // 결제 시작 (Wallet 차감 대기)
    PAID,                  // 결제 완료 (Wallet holding 보관)
    SHIPPING,              // 배송 중
    DELIVERED,             // 배송 완료
    CONFIRMED,             // 구매 확정 — 거래 종결 (정산 흐름은 추후 별도 PR에서 도입)
    CANCELLED,             // 결제 전 취소
    REFUND_PROCESSING,     // 결제 후 취소, 환불 처리 중
    REFUND_COMPLETED,      // 환불 완료
    RETURN_REQUESTED,      // 반송 요청 (배송 시작 후만 가능)
    RETURN_APPROVED,       // 반송 승인 (이후 OrderReturnStatus가 추적)
    RETURN_REJECTED;       // 반송 거절

    // 종결 상태 — 더 이상 OrderTransition 표에 out-going 전이 없음
    public boolean isTerminal() {
        return this == CONFIRMED
                || this == CANCELLED
                || this == REFUND_COMPLETED
                || this == RETURN_REJECTED
                || this == RETURN_APPROVED;
    }
}
