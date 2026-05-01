package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.RequestReturnUseCase.RequestReturnCommand;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestReturnServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @InjectMocks RequestReturnService service;

    @Test
    @DisplayName("SHIPPING → RETURN_REQUESTED + history 기록")
    void requestReturn() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.shippingOrder(buyerId, UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.requestReturn(new RequestReturnCommand(order.getId().value(), buyerId, "불량품"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REQUESTED);
        assertThat(order.getReturnReason().value()).isEqualTo("불량품");
        verify(historyRecorder).record(order.getId(), OrderStatus.SHIPPING, OrderStatus.RETURN_REQUESTED, order.getReturnReason());
        verify(orderRepository).save(order);
    }
}
