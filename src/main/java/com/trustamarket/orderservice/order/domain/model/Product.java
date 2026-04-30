package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;
import com.trustamarket.orderservice.order.domain.exception.InvalidNameException;

import java.util.UUID;

// 상품 스냅샷 — 주문 시점의 id + name + price를 보존
// Product 도메인의 이름/가격이 추후 변경되어도 Order의 product 정보는 변하지 않음
// 정산은 이 시점 가격(price) 기준으로 이루어짐
public record Product(UUID id, String name, Money price) {

    public Product {
        if (id == null) {
            throw new InvalidIdException("productId");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidNameException("productName");
        }
        if (price == null) {
            throw new InvalidMoneyException("productPrice");
        }
    }

    public static Product of(UUID id, String name, Money price) {
        return new Product(id, name, price);
    }
}
