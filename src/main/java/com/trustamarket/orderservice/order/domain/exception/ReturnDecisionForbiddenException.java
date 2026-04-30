package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 403 Forbidden — ADMIN 외 권한이 반송 승인/거절 시도
public class ReturnDecisionForbiddenException extends OrderException {

    public ReturnDecisionForbiddenException() {
        super(HttpStatus.FORBIDDEN, "반송 승인/거절 결정 권한이 없습니다. ADMIN 전용입니다.");
    }
}
