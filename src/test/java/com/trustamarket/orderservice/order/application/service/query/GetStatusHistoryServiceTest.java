package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.port.out.OrderStatusHistoryRepository;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStatusHistoryServiceTest {

    @Mock OrderStatusHistoryRepository historyRepository;
    @InjectMocks GetStatusHistoryService service;

    @Test
    @DisplayName("orderId로 historyRepository.findByOrderId 위임")
    void delegatesToRepository() {
        UUID orderId = UUID.randomUUID();
        when(historyRepository.findByOrderId(OrderId.of(orderId))).thenReturn(List.of());

        service.getHistory(orderId);

        verify(historyRepository).findByOrderId(OrderId.of(orderId));
    }
}
