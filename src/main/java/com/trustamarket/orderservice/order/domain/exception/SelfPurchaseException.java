package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — 자기 상품 구매 시도 (도메인 invariant 위반)
public class SelfPurchaseException extends OrderException {

    public SelfPurchaseException() {
        super(HttpStatus.BAD_REQUEST, "자신의 상품은 구매할 수 없습니다.", "buyerId");
    }
}
