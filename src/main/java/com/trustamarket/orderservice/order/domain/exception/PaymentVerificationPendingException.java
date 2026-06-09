package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

// 202 Accepted — 결제 결과 확인이 즉시 불가하여 비동기 재처리 (reconciliation) 위임.
// saga 의 wallet deduct 가 timeout / 5xx 응답을 받았고, 후속 getUsage 도 실패한 케이스.
// reconciliation 큐에 등록된 상태이므로 @Scheduled 가 백오프 (30s/60s/120s) 로 재시도한다.
// 사용자에게 "결제 처리 중" 의미로 응답.
public class PaymentVerificationPendingException extends OrderException {

    private static final String USER_MESSAGE = "결제 처리 중입니다. 잠시 후 주문 내역에서 확인해주세요.";

    public PaymentVerificationPendingException(UUID orderId, Throwable cause) {
        super(HttpStatus.ACCEPTED, USER_MESSAGE, "order/" + orderId);
        initCause(cause);
    }
}
