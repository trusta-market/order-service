package com.trustamarket.orderservice.order.domain.exception;

import com.trustamarket.common.exception.CustomException;
import org.springframework.http.HttpStatus;

// Order 도메인의 모든 비즈니스 예외의 base 에러
// 비즈니스별 구체 클래스(OrderNotFoundException 등)가 이를 상속함
// Catch by type 가능하도록 abstract로 class 설정했음!
public abstract class OrderException extends CustomException {

    protected OrderException(HttpStatus status, String message) {
        super(status, message);
    }

    protected OrderException(HttpStatus status, String message, String field) {
        super(status, message, field);
    }
}
