package com.trustamarket.orderservice.order.domain.exception;

import com.trustamarket.orderservice.order.domain.model.Reason;
import org.springframework.http.HttpStatus;

// 400 Bad Request — Reason VO 검증 실패 (blank 또는 max length 초과)
// max length는 Reason.MAX_LENGTH를 단일 소스로 참조 (이중 관리 방지)
public class InvalidReasonException extends OrderException {

    public InvalidReasonException() {
        super(HttpStatus.BAD_REQUEST,
                "사유는 비어있을 수 없으며 최대 %d자입니다.".formatted(Reason.MAX_LENGTH),
                "reason");
    }
}
