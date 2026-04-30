package com.trustamarket.orderservice.order.domain.exception;

import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import org.springframework.http.HttpStatus;

// 409 Conflict — 배송 시작 후/종결 상태에서 취소 시도
public class CancelNotAllowedException extends OrderException {

    public CancelNotAllowedException(OrderStatus current) {
        super(HttpStatus.CONFLICT,
                "현재 상태(%s)에서는 취소가 불가합니다. 배송 시작 후에는 반송을 사용하세요.".formatted(current),
                "status");
    }
}
