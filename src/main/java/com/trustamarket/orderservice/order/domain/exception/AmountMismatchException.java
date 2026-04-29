package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — totalAmount != productPrice + shippingFee (도메인 invariant 위반)
public class AmountMismatchException extends OrderException {

    public AmountMismatchException(long expected, long actual) {
        super(HttpStatus.BAD_REQUEST,
                "총액 계산이 맞지 않습니다. expected=%d, actual=%d".formatted(expected, actual),
                "totalAmount");
    }
}
