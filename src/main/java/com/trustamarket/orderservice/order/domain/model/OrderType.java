package com.trustamarket.orderservice.order.domain.model;

public enum OrderType {
    LOW,    // 저가 상품 (검수 없음)
    HIGH    // 고가 상품 (검수 후 ON_SALE 도달한 상품)
}
