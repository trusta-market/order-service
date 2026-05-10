package com.trustamarket.orderservice.order.domain.model;

// Order 상태 머신의 액션 (OrderTransition 이 사용)
public enum OrderAction {

    REQUEST_PAYMENT,      // REQUESTED → PAYMENT_PENDING
    MARK_PAID,            // PAYMENT_PENDING → PAID
    START_SHIPPING,       // PAID → SHIPPING
    MARK_DELIVERED,       // SHIPPING → DELIVERED
    CONFIRM,              // DELIVERED → CONFIRMED (거래 종결)

    CANCEL,               // (결제 전) → CANCELLED, (PAID) → CANCELLATION_PROCESSING
    MARK_CANCELLED,       // CANCELLATION_PROCESSING → CANCELLATION_COMPLETED (wallet.cancellation.completed 수신)

    REQUEST_RETURN,       // (배송 시작 후) → RETURN_REQUESTED
    APPROVE_RETURN,       // RETURN_REQUESTED → RETURN_APPROVED
    REJECT_RETURN         // RETURN_REQUESTED → RETURN_REJECTED
}
