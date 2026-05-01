package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.port.in.GetMySalesUseCase.GetMySalesQuery;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.domain.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMySalesServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks GetMySalesService service;

    @Test
    @DisplayName("sellerId로 Repository.findBySellerId 호출")
    void delegatesToRepository() {
        UUID sellerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> empty = new PageImpl<>(List.of());
        when(orderRepository.findBySellerId(sellerId, pageable)).thenReturn(empty);

        service.getMySales(new GetMySalesQuery(sellerId, pageable));

        verify(orderRepository).findBySellerId(sellerId, pageable);
    }
}
