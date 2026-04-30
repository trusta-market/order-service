package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — Snapshot VO(BuyerSnapshot/SellerSnapshot/ProductSnapshot)의 name 필드 검증 실패
public class InvalidNameException extends OrderException {

    public InvalidNameException(String fieldName) {
        super(HttpStatus.BAD_REQUEST,
                "%s은(는) 비어있을 수 없습니다.".formatted(fieldName),
                fieldName);
    }
}
