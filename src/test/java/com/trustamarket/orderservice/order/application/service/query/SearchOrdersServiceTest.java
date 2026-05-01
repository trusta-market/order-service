package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.port.in.SearchOrdersUseCase.SearchOrdersQuery;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.port.out.query.OrderSearchCriteria;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
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
class SearchOrdersServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks SearchOrdersService service;

    @Test
    @DisplayName("Repository.search(criteria, pageable)로 위임")
    void delegatesToRepository() {
        OrderSearchCriteria criteria = new OrderSearchCriteria(OrderStatus.PAID, null, null, "홍길동");
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> empty = new PageImpl<>(List.of());
        when(orderRepository.search(criteria, pageable)).thenReturn(empty);

        service.search(new SearchOrdersQuery(criteria, pageable));

        verify(orderRepository).search(criteria, pageable);
    }
}
