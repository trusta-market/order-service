package com.trustamarket.orderservice.order.domain.model;

// 반송 진행 추적 (p_order_return.return_status에 매핑)
public enum OrderReturnStatus {

    RETURN_REQUESTED,     // 반송 요청
    RETURN_APPROVED,      // 반송 승인 (ADMIN 결정)
    RETURN_REJECTED,      // 반송 거절 (ADMIN 결정)
    RETURN_COLLECTING,    // 구매자 → 센터 반송 배송 중
    RETURN_RECEIVED,      // 센터 도착
    RETURN_INSPECTING,    // 재검수 중
    RETURN_COMPLETED      // 반송 완료 (이후 취소 흐름은 별도 PR)
}
