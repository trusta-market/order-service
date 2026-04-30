package com.trustamarket.orderservice.order.domain.exception;

import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import org.springframework.http.HttpStatus;

// 400 Bad Request — 복원된 주문의 상태별 메타데이터 불변식 위반
// 정상 도메인 행위 메서드를 거치면 발생할 수 없는 조합 → DB 변조 감지
// 메시지·field는 정적 팩터리에서 고정 (호출부 자유 문자열 금지)
public class RestoreStateMismatchException extends OrderException {

    private RestoreStateMismatchException(String message, String field) {
        super(HttpStatus.BAD_REQUEST, message, field);
    }

    public static RestoreStateMismatchException missingConfirmedAt(OrderStatus status) {
        return new RestoreStateMismatchException(
                "%s 상태는 confirmedAt이 필수입니다.".formatted(status),
                "confirmedAt");
    }

    public static RestoreStateMismatchException missingCancelReason(OrderStatus status) {
        return new RestoreStateMismatchException(
                "%s 상태는 cancelReason이 필수입니다.".formatted(status),
                "cancelReason");
    }

    public static RestoreStateMismatchException missingReturnReason(OrderStatus status) {
        return new RestoreStateMismatchException(
                "%s 상태는 returnReason이 필수입니다.".formatted(status),
                "returnReason");
    }

    public static RestoreStateMismatchException missingRejectReason() {
        return new RestoreStateMismatchException(
                "RETURN_REJECTED 상태는 rejectReason이 필수입니다.",
                "rejectReason");
    }

    public static RestoreStateMismatchException inconsistentDeletionMetadata() {
        return new RestoreStateMismatchException(
                "deletedAt/deletedBy는 동시에 채워져야 합니다.",
                "deletedAt");
    }
}
