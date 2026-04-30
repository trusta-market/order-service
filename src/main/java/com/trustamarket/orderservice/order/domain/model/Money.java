package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;

// 금액 VO, 원화 정수 기준 (long)
// 음수 거부 + plus/minus 메서드 (null 인자는 도메인 예외로 차단 — NPE 회피)
public record Money(long value) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (value < 0) {
            throw new InvalidMoneyException(value);
        }
    }

    public static Money of(long value) {
        return new Money(value);
    }

    public Money plus(Money other) {
        if (other == null) {
            throw new InvalidMoneyException("other");
        }
        return new Money(this.value + other.value);
    }

    public Money minus(Money other) {
        if (other == null) {
            throw new InvalidMoneyException("other");
        }
        return new Money(this.value - other.value);
    }

    public boolean equalsAmount(Money other) {
        if (other == null) {
            throw new InvalidMoneyException("other");
        }
        return this.value == other.value;
    }
}
