package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.dto.result.CreateOrderResult;
import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderHistoryRecorder historyRecorder;
    @InjectMocks CreateOrderService service;

    @Test
    @DisplayName("주문 생성 — happy path")
    void createOrder() {
        CreateOrderCommand cmd = new CreateOrderCommand(
                UUID.randomUUID(), "구매자",
                UUID.randomUUID(), "판매자",
                UUID.randomUUID(), "상품", 100_000L,
                OrderType.LOW,
                3_000L
        );
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateOrderResult result = service.createOrder(cmd);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.REQUESTED);
        assertThat(result.totalAmount()).isEqualTo(103_000L);
        verify(orderRepository).save(any(Order.class));
        // 신규 생성: prev=null, next=REQUESTED
        verify(historyRecorder).record(any(OrderId.class), eq(null), eq(OrderStatus.REQUESTED), eq(null));
    }
}
