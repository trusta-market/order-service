package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
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
}
