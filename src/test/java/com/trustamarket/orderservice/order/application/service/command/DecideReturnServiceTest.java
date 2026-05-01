package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase.DecideReturnCommand;
import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase.Decision;
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
class DecideReturnServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @InjectMocks DecideReturnService service;

    @Test
    @DisplayName("APPROVE → RETURN_APPROVED")
    void approve() {
        Order order = OrderTestFixtures.returnRequestedOrder(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.decide(new DecideReturnCommand(
                order.getId().value(), UUID.randomUUID(), Decision.APPROVE, null
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_APPROVED);
        verify(historyRecorder).record(order.getId(), OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED, null);
    }

    @Test
    @DisplayName("REJECT → RETURN_REJECTED + rejectReason 기록")
    void reject() {
        Order order = OrderTestFixtures.returnRequestedOrder(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        service.decide(new DecideReturnCommand(
                order.getId().value(), UUID.randomUUID(), Decision.REJECT, "증빙 부족"
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RETURN_REJECTED);
        assertThat(order.getRejectReason().value()).isEqualTo("증빙 부족");
    }
}
