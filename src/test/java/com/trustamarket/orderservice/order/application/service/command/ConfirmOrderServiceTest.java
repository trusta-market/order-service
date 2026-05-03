package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.application.port.in.ConfirmOrderUseCase.ConfirmOrderCommand;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.SettlementMessagePort;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @Mock SettlementMessagePort settlementPublisher;
    @InjectMocks ConfirmOrderService service;

    @Test
    @DisplayName("DELIVERED → CONFIRMED + history 기록")
    void confirm() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.shippingOrder(buyerId, UUID.randomUUID());
        order.markDelivered();   // SHIPPING → DELIVERED
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.confirm(new ConfirmOrderCommand(order.getId().value(), buyerId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getConfirmedAt()).isNotNull();
        verify(historyRecorder).record(order.getId(), OrderStatus.DELIVERED, OrderStatus.CONFIRMED, null);
        verify(orderRepository).save(order);
        verify(settlementPublisher).publishForPaidOrder(order);
    }

    @Test
    @DisplayName("PAID → CONFIRMED 시연 우회 — 정산 발행 (배송 단계 skip)")
    void confirm_skipShippingFromPaid() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.paidOrder(buyerId, UUID.randomUUID());  // REQUESTED → PAYMENT_PENDING → PAID
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.confirm(new ConfirmOrderCommand(order.getId().value(), buyerId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(historyRecorder).record(order.getId(), OrderStatus.PAID, OrderStatus.CONFIRMED, null);
        verify(settlementPublisher).publishForPaidOrder(order);
    }

    @Test
    @DisplayName("buyer 아닌 사용자 → UnauthorizedOrderAccessException")
    void unauthorized() {
        UUID buyerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        Order order = OrderTestFixtures.shippingOrder(buyerId, UUID.randomUUID());
        order.markDelivered();
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        assertThatThrownBy(() ->
                service.confirm(new ConfirmOrderCommand(order.getId().value(), strangerId))
        ).isInstanceOf(UnauthorizedOrderAccessException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(historyRecorder, never()).record(any(), any(), any(), eq(null));
    }
}
