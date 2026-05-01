package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — enum 필드 값이 부적절 (null 등)
// OrderStatus / OrderType / Decision 등 enum 타입의 필수 검증 실패에 사용
// 식별자(InvalidIdException)나 상태 전이(InvalidStatusTransitionException)와 구분
public class InvalidEnumException extends OrderException {

    public InvalidEnumException(String field) {
        super(HttpStatus.BAD_REQUEST,
                "값(%s)이 부적절합니다.".formatted(field),
                field);
    }
}
