package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.application.port.in.GetMyOrdersUseCase.GetMyOrdersQuery;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.domain.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class GetMyOrdersServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks GetMyOrdersService service;

    @Test
    @DisplayName("buyer 또는 seller로 참여한 주문 통합 조회 — Repository에 actor가 buyerId/sellerId 둘 다로 호출됨")
    void unifiedQuery() {
        UUID actor = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> empty = new PageImpl<>(List.of());
        when(orderRepository.findByBuyerIdOrSellerId(actor, actor, pageable)).thenReturn(empty);

        Page<OrderSummaryView> result = service.getMyOrders(new GetMyOrdersQuery(actor, pageable));

        assertThat(result.getContent()).isEmpty();
        verify(orderRepository).findByBuyerIdOrSellerId(actor, actor, pageable);
    }
}
