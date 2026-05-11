package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTransitionTest {

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("validTransitions")
    @DisplayName("허용된 전이는 다음 상태를 반환")
    void validTransition(OrderStatus from, OrderAction action, OrderStatus expected) {
        assertThat(OrderTransition.apply(from, action)).isEqualTo(expected);
    }

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                // happy path
                Arguments.of(OrderStatus.REQUESTED, OrderAction.REQUEST_PAYMENT, OrderStatus.PAYMENT_PENDING),
                Arguments.of(OrderStatus.PAYMENT_PENDING, OrderAction.MARK_PAID, OrderStatus.PAID),
                Arguments.of(OrderStatus.PAID, OrderAction.START_SHIPPING, OrderStatus.SHIPPING),
                Arguments.of(OrderStatus.SHIPPING, OrderAction.MARK_DELIVERED, OrderStatus.DELIVERED),
                Arguments.of(OrderStatus.DELIVERED, OrderAction.CONFIRM, OrderStatus.CONFIRMED),
                // cancel
                Arguments.of(OrderStatus.REQUESTED, OrderAction.CANCEL, OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.PAYMENT_PENDING, OrderAction.CANCEL, OrderStatus.CANCELLED),
                Arguments.of(OrderStatus.PAID, OrderAction.CANCEL, OrderStatus.CANCELLATION_PROCESSING),
                // cancellation
                Arguments.of(OrderStatus.CANCELLATION_PROCESSING, OrderAction.MARK_CANCELLED, OrderStatus.CANCELLATION_COMPLETED),
                // return
                Arguments.of(OrderStatus.SHIPPING, OrderAction.REQUEST_RETURN, OrderStatus.RETURN_REQUESTED),
                Arguments.of(OrderStatus.DELIVERED, OrderAction.REQUEST_RETURN, OrderStatus.RETURN_REQUESTED),
                Arguments.of(OrderStatus.RETURN_REQUESTED, OrderAction.APPROVE_RETURN, OrderStatus.RETURN_APPROVED),
                Arguments.of(OrderStatus.RETURN_REQUESTED, OrderAction.REJECT_RETURN, OrderStatus.RETURN_REJECTED)
        );
    }

    @ParameterizedTest(name = "{0} + {1} -> 차단")
    @MethodSource("invalidTransitions")
    @DisplayName("허용되지 않은 전이는 InvalidStatusTransitionException")
    void invalidTransition(OrderStatus from, OrderAction action) {
        assertThatThrownBy(() -> OrderTransition.apply(from, action))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                // 종결 상태에서의 모든 액션
                Arguments.of(OrderStatus.CONFIRMED, OrderAction.CANCEL),
                Arguments.of(OrderStatus.CANCELLED, OrderAction.REQUEST_PAYMENT),
                Arguments.of(OrderStatus.CANCELLATION_COMPLETED, OrderAction.CANCEL),
                Arguments.of(OrderStatus.RETURN_REJECTED, OrderAction.APPROVE_RETURN),
                // 배송 시작 후 취소 차단
                Arguments.of(OrderStatus.SHIPPING, OrderAction.CANCEL),
                Arguments.of(OrderStatus.DELIVERED, OrderAction.CANCEL),
                // 배송 시작 전 반송 요청 차단
                Arguments.of(OrderStatus.REQUESTED, OrderAction.REQUEST_RETURN),
                Arguments.of(OrderStatus.PAID, OrderAction.REQUEST_RETURN),
                // 잘못된 happy path 점프
                Arguments.of(OrderStatus.REQUESTED, OrderAction.MARK_PAID),
                Arguments.of(OrderStatus.PAID, OrderAction.MARK_DELIVERED),
                // RETURN_APPROVED는 OrderTransition 표에서 다음 액션 없음 (OrderReturnStatus 추적)
                Arguments.of(OrderStatus.RETURN_APPROVED, OrderAction.APPROVE_RETURN)
        );
    }
}
