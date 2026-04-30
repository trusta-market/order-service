package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;

// 사유 VO — 취소/반송/거절 사유 보관
// 비어있을 수 없고 최대 200자
public record Reason(String value) {

    public static final int MAX_LENGTH = 200;

    public Reason {
        if (value == null || value.isBlank()) {
            throw new InvalidReasonException();
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidReasonException();
        }
    }

    public static Reason of(String value) {
        return new Reason(value);
    }
}
