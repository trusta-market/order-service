package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 502 Bad Gateway — Wallet 동기 호출 자체가 실패 (네트워크 오류, 5xx 응답 등)
// 잔액 부족(InsufficientPointBalanceException)과는 구분 — 이건 시스템 오류
// 사용자 노출 메시지는 고정 문구 (외부 응답 원문/내부 오류 정보 누수 차단)
// 상세 원인은 호출부에서 logger로 별도 기록
public class WalletCommunicationException extends OrderException {

    private static final String USER_MESSAGE = "결제 처리 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";

    public WalletCommunicationException() {
        super(HttpStatus.BAD_GATEWAY, USER_MESSAGE, "wallet");
    }

    // cause 보존 — 상세는 stack trace로 확인. 사용자 노출 메시지는 변하지 않음
    public WalletCommunicationException(Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, USER_MESSAGE, "wallet");
        initCause(cause);
    }
}
