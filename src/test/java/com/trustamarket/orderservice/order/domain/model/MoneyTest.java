package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidMoneyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("of()로 생성한다")
    void create() {
        Money money = Money.of(10_000);
        assertThat(money.value()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("ZERO 상수는 0원이다")
    void zero() {
        assertThat(Money.ZERO.value()).isZero();
    }

    @Test
    @DisplayName("0원도 허용한다")
    void allowZero() {
        assertThat(Money.of(0).value()).isZero();
    }

    @Test
    @DisplayName("음수는 거부한다")
    void rejectNegative() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("plus는 두 금액을 더한다")
    void plus() {
        Money result = Money.of(10_000).plus(Money.of(3_000));
        assertThat(result.value()).isEqualTo(13_000);
    }

    @Test
    @DisplayName("minus는 두 금액을 뺀다")
    void minus() {
        Money result = Money.of(10_000).minus(Money.of(3_000));
        assertThat(result.value()).isEqualTo(7_000);
    }

    @Test
    @DisplayName("minus 결과가 음수면 거부한다")
    void minusNegative() {
        assertThatThrownBy(() -> Money.of(1_000).minus(Money.of(2_000)))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("equalsAmount는 값이 같을 때 true")
    void equalsAmount() {
        assertThat(Money.of(1_000).equalsAmount(Money.of(1_000))).isTrue();
        assertThat(Money.of(1_000).equalsAmount(Money.of(2_000))).isFalse();
    }

    @Test
    @DisplayName("plus에 null 인자 → InvalidMoneyException")
    void plusNull() {
        assertThatThrownBy(() -> Money.of(1_000).plus(null))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("minus에 null 인자 → InvalidMoneyException")
    void minusNull() {
        assertThatThrownBy(() -> Money.of(1_000).minus(null))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("equalsAmount에 null 인자 → InvalidMoneyException")
    void equalsAmountNull() {
        assertThatThrownBy(() -> Money.of(1_000).equalsAmount(null))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("plus 결과가 long 범위 초과 시 InvalidMoneyException (음수 wrap-around 차단)")
    void plusOverflow() {
        Money max = Money.of(Long.MAX_VALUE);
        assertThatThrownBy(() -> max.plus(Money.of(1)))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    @DisplayName("minus 결과가 음수면 InvalidMoneyException (Math.subtractExact는 long underflow도 방어)")
    void minusUnderflow() {
        Money zero = Money.ZERO;
        assertThatThrownBy(() -> zero.minus(Money.of(1)))
                .isInstanceOf(InvalidMoneyException.class);
    }
}
