package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.exception.InvalidStatusTransitionException;
import com.trustamarket.orderservice.order.domain.exception.OrderNotFoundException;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkOrderCancelledServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @InjectMocks MarkOrderCancelledService service;

    @Test
    @DisplayName("CANCELLATION_PROCESSING → CANCELLATION_COMPLETED + history 기록")
    void markCancelled() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.paidOrder(buyerId, UUID.randomUUID());
        order.cancel(Reason.of("buyer 변심"));   // PAID → CANCELLATION_PROCESSING
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.markCancelled(order.getId().value());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLATION_COMPLETED);
        verify(historyRecorder).record(order.getId(),
                OrderStatus.CANCELLATION_PROCESSING, OrderStatus.CANCELLATION_COMPLETED, null);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Order 없음 → OrderNotFoundException + 부수효과 X")
    void orderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdOrThrow(OrderId.of(orderId)))
                .thenThrow(new OrderNotFoundException(orderId));

        assertThatThrownBy(() -> service.markCancelled(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(historyRecorder, never()).record(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 CANCELLATION_COMPLETED 인 주문은 재호출 시 InvalidStatusTransitionException")
    void alreadyCancelled_invalidTransition() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.paidOrder(buyerId, UUID.randomUUID());
        order.cancel(Reason.of("buyer 변심"));
        order.markCancelled();   // CANCELLATION_PROCESSING → CANCELLATION_COMPLETED
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        assertThatThrownBy(() -> service.markCancelled(order.getId().value()))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(orderRepository, never()).save(any());
        verify(historyRecorder, never()).record(any(), any(), any(), any());
    }
}
