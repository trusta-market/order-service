package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidReasonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReasonTest {

    @Test
    @DisplayName("of()로 사유를 생성한다")
    void create() {
        Reason reason = Reason.of("단순 변심");
        assertThat(reason.value()).isEqualTo("단순 변심");
    }

    @Test
    @DisplayName("null 사유는 거부한다")
    void rejectNull() {
        assertThatThrownBy(() -> Reason.of(null))
                .isInstanceOf(InvalidReasonException.class);
    }

    @Test
    @DisplayName("빈 문자열 사유는 거부한다")
    void rejectBlank() {
        assertThatThrownBy(() -> Reason.of(""))
                .isInstanceOf(InvalidReasonException.class);
        assertThatThrownBy(() -> Reason.of("   "))
                .isInstanceOf(InvalidReasonException.class);
    }

    @Test
    @DisplayName("최대 길이까지는 허용한다")
    void allowMaxLength() {
        String maxValue = "a".repeat(Reason.MAX_LENGTH);
        Reason reason = Reason.of(maxValue);
        assertThat(reason.value()).hasSize(Reason.MAX_LENGTH);
    }

    @Test
    @DisplayName("최대 길이를 초과하면 거부한다")
    void rejectOverMaxLength() {
        String overValue = "a".repeat(Reason.MAX_LENGTH + 1);
        assertThatThrownBy(() -> Reason.of(overValue))
                .isInstanceOf(InvalidReasonException.class);
    }
}
