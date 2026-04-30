package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — 도메인 메서드의 시각 인자(Instant) null 차단
public class InvalidTimestampException extends OrderException {

    public InvalidTimestampException(String field) {
        super(HttpStatus.BAD_REQUEST,
                "시각(%s)은 null일 수 없습니다.".formatted(field),
                field);
    }
}
