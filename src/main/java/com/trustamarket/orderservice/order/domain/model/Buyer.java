package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.exception.InvalidNameException;

import java.util.UUID;

// 구매자 snapshot — 주문 시점의 id + name을 보존
// User 도메인의 이름이 추후 변경되어도 Order의 buyer 정보는 변하지 않음 (audit/무결성)
public record Buyer(UUID id, String name) {

    public Buyer {
        if (id == null) {
            throw new InvalidIdException("buyerId");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidNameException("buyerName");
        }
    }

    public static Buyer of(UUID id, String name) {
        return new Buyer(id, name);
    }
}
