package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

// 404 Not Found — 주문 리소스 부재
public class OrderNotFoundException extends OrderException {

    public OrderNotFoundException(UUID orderId) {
        super(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다: " + orderId);
    }
}
