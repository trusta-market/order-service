package com.trustamarket.orderservice.order.domain.exception;

import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import org.springframework.http.HttpStatus;

// 409 Conflict — 배송 시작 전/종결 상태에서 반송 요청 시도
public class ReturnNotAllowedException extends OrderException {

    public ReturnNotAllowedException(OrderStatus current) {
        super(HttpStatus.CONFLICT,
                "현재 상태(%s)에서는 반송 요청이 불가합니다. 배송 시작 후에만 가능합니다.".formatted(current),
                "status");
    }
}
