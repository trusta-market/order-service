package com.trustamarket.orderservice.order.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    // 종결 상태: OrderTransition 표에 out-going 전이가 없는 상태
    private static final Set<OrderStatus> TERMINAL = Set.of(
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED,
            OrderStatus.REFUND_COMPLETED,
            OrderStatus.RETURN_REJECTED,
            OrderStatus.RETURN_APPROVED
    );

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("isTerminal()은 표 기반 종결 상태와 일치한다")
    void isTerminal(OrderStatus status) {
        assertThat(status.isTerminal()).isEqualTo(TERMINAL.contains(status));
    }
}
