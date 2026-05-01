package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.port.in.GetMyPurchasesUseCase.GetMyPurchasesQuery;
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
class GetMyPurchasesServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks GetMyPurchasesService service;

    @Test
    @DisplayName("buyerId로 Repository.findByBuyerId 호출")
    void delegatesToRepository() {
        UUID buyerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> empty = new PageImpl<>(List.of());
        when(orderRepository.findByBuyerId(buyerId, pageable)).thenReturn(empty);

        service.getMyPurchases(new GetMyPurchasesQuery(buyerId, pageable));

        verify(orderRepository).findByBuyerId(buyerId, pageable);
    }
}
