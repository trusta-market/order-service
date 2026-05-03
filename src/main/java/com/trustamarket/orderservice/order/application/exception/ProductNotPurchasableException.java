package com.trustamarket.orderservice.order.application.exception;

import com.trustamarket.orderservice.order.domain.exception.OrderException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

// 상품이 ON_SALE 상태가 아니라 주문 불가능 — 409 Conflict.
// product-service 의 ProductStatus 가 PENDING_INSPECTION / RESERVED / SOLD_OUT 인 경우.
public class ProductNotPurchasableException extends OrderException {

    public ProductNotPurchasableException(UUID productId, String currentStatus) {
        super(HttpStatus.CONFLICT,
                "주문할 수 없는 상품입니다. 현재 상태: " + currentStatus,
                "productId=" + productId);
    }
}
