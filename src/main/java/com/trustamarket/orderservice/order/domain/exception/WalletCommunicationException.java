package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 502 Bad Gateway — Wallet 동기 호출 자체가 실패 (네트워크 오류, 5xx 응답 등)
// 잔액 부족(InsufficientPointBalanceException)과는 구분 — 이건 시스템 오류
public class WalletCommunicationException extends OrderException {

    public WalletCommunicationException(String detail) {
        super(HttpStatus.BAD_GATEWAY,
                "Wallet 통신 실패: " + detail,
                "wallet");
    }
}
