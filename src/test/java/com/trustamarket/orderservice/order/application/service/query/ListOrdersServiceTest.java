package com.trustamarket.orderservice.order.application.service.query;

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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListOrdersServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks ListOrdersService service;

    @Test
    @DisplayName("Repository.findAll(pageable)로 위임")
    void delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> empty = new PageImpl<>(List.of());
        when(orderRepository.findAll(pageable)).thenReturn(empty);

        service.list(pageable);

        verify(orderRepository).findAll(pageable);
    }
}
