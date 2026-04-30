package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;

import java.util.UUID;

public record OrderReturnId(UUID value) {

    public OrderReturnId {
        if (value == null) {
            throw new InvalidIdException("orderReturnId");
        }
    }

    public static OrderReturnId generate() {
        return new OrderReturnId(UUID.randomUUID());
    }

    public static OrderReturnId of(UUID value) {
        return new OrderReturnId(value);
    }
}
