package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

// 400 Bad Request — Money VO 검증 실패 (음수 또는 null)
public class InvalidMoneyException extends OrderException {

    public InvalidMoneyException(long value) {
        super(HttpStatus.BAD_REQUEST,
                "금액은 0 이상이어야 합니다. 입력값: " + value,
                "amount");
    }

    public InvalidMoneyException(String field) {
        super(HttpStatus.BAD_REQUEST,
                "금액(%s)은 null일 수 없습니다.".formatted(field),
                field);
    }
}
