package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — Reason VO 검증 실패 (blank 또는 200자 초과)
public class InvalidReasonException extends OrderException {

    private static final int MAX_LENGTH = 200;

    public InvalidReasonException() {
        super(HttpStatus.BAD_REQUEST,
                "사유는 비어있을 수 없으며 최대 %d자입니다.".formatted(MAX_LENGTH),
                "reason");
    }
}
