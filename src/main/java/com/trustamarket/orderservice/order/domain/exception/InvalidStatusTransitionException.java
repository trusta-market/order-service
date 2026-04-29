package com.trustamarket.orderservice.order.domain.exception;

import com.trustamarket.orderservice.order.domain.model.OrderAction;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import org.springframework.http.HttpStatus;

// 409 Conflict — 허용되지 않는 상태 전이 시도
public class InvalidStatusTransitionException extends OrderException {

    public InvalidStatusTransitionException(OrderStatus from, OrderAction action) {
        super(HttpStatus.CONFLICT,
                "%s 상태에서 %s 액션은 허용되지 않습니다.".formatted(from, action),
                "status");
    }
}
