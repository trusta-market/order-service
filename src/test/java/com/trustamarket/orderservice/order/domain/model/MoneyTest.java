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
}
