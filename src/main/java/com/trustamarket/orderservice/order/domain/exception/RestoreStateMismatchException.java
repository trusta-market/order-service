package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — 복원된 주문의 상태별 메타데이터 불변식 위반
// 정상 도메인 행위 메서드를 거치면 발생할 수 없는 조합 → DB 변조 감지
// (예: CONFIRMED인데 confirmedAt == null, CANCELLED인데 cancelReason == null,
//      deletedAt/deletedBy가 반쪽만 채워진 상태 등)
public class RestoreStateMismatchException extends OrderException {

    public RestoreStateMismatchException(String detail) {
        super(HttpStatus.BAD_REQUEST,
                "복원된 주문의 상태와 메타데이터가 일치하지 않습니다: " + detail,
                "status");
    }
}
