package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.application.port.in.CancelOrderUseCase.CancelOrderCommand;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
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
class CancelOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @InjectMocks CancelOrderService service;

    @Test
    @DisplayName("REQUESTED 단계 취소 → CANCELLED + history 기록")
    void cancelRequested() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.cancel(new CancelOrderCommand(order.getId().value(), buyerId, "변심"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(historyRecorder).record(order.getId(), OrderStatus.REQUESTED, OrderStatus.CANCELLED, order.getCancelReason());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("PAID 단계 취소 → CANCELLATION_PROCESSING (markCancelled는 후속 PR)")
    void cancelPaid() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.paidOrder(buyerId, UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.cancel(new CancelOrderCommand(order.getId().value(), buyerId, "환불 요청"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLATION_PROCESSING);
        verify(historyRecorder).record(order.getId(), OrderStatus.PAID, OrderStatus.CANCELLATION_PROCESSING, order.getCancelReason());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("buyer 아닌 사용자 → UnauthorizedOrderAccessException")
    void unauthorized() {
        UUID buyerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        assertThatThrownBy(() ->
                service.cancel(new CancelOrderCommand(order.getId().value(), strangerId, "변심"))
        ).isInstanceOf(UnauthorizedOrderAccessException.class);

        verify(orderRepository, never()).save(order);
        verify(historyRecorder, never()).record(any(), any(), any(), any());
    }
}
