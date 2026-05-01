package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — 상태값 자체가 부적절 (null 등)
// 식별자 검증(InvalidIdException)이나 전이 검증(InvalidStatusTransitionException)과 구분
public class InvalidStatusException extends OrderException {

    public InvalidStatusException(String field) {
        super(HttpStatus.BAD_REQUEST,
                "상태값(%s)이 부적절합니다.".formatted(field),
                field);
    }
}
