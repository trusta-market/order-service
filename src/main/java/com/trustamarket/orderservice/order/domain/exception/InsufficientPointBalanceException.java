package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — Wallet 결제 시 잔액 부족
// application service가 Wallet 응답의 shortage > 0을 받으면 변환해서 throw
public class InsufficientPointBalanceException extends OrderException {

    public InsufficientPointBalanceException(long required, long balance, long shortage) {
        super(HttpStatus.BAD_REQUEST,
                "포인트 잔액이 부족합니다. 필요: %d, 보유: %d, 부족: %d".formatted(required, balance, shortage),
                "balance");
    }
}
