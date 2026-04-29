package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — Money VO 음수 검증 실패
public class InvalidMoneyException extends OrderException {

    public InvalidMoneyException(long value) {
        super(HttpStatus.BAD_REQUEST,
                "금액은 0 이상이어야 합니다. 입력값: " + value,
                "amount");
    }
}
