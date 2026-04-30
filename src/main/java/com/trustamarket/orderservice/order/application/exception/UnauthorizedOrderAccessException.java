package com.trustamarket.orderservice.order.application.exception;

import com.trustamarket.common.exception.CustomException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

// 403 Forbidden — application 영역 권한 검증 실패
// 도메인 예외(OrderException) 대신 application 예외로 분리 — 권한은 application 책임
public class UnauthorizedOrderAccessException extends CustomException {

    public UnauthorizedOrderAccessException(UUID orderId, UUID actorId) {
        super(HttpStatus.FORBIDDEN,
                "주문 %s에 대한 접근 권한이 없습니다 (actor: %s)".formatted(orderId, actorId),
                "actor");
    }
}
