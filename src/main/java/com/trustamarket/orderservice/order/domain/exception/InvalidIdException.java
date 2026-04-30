package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — 식별자 VO(OrderId/OrderReturnId/BuyerId/SellerId/ProductId)의 value null 검증 실패
public class InvalidIdException extends OrderException {

    public InvalidIdException(String fieldName) {
        super(HttpStatus.BAD_REQUEST,
                "%s 값은 필수입니다.".formatted(fieldName),
                fieldName);
    }
}
