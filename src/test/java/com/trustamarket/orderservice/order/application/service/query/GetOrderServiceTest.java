package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.exception.UnauthorizedOrderAccessException;
import com.trustamarket.orderservice.order.application.port.in.GetOrderUseCase.GetOrderQuery;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.domain.model.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @InjectMocks GetOrderService service;

    @Test
    @DisplayName("buyer 본인 조회 — 성공")
    void buyerCanGet() {
        UUID buyerId = UUID.randomUUID();
        Order order = OrderTestFixtures.requestedOrder(buyerId, UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        Order result = service.getOrder(new GetOrderQuery(order.getId().value(), buyerId));
        assertThat(result).isSameAs(order);
    }

    @Test
    @DisplayName("seller 본인 조회 — 성공")
    void sellerCanGet() {
        UUID sellerId = UUID.randomUUID();
        Order order = OrderTestFixtures.requestedOrder(UUID.randomUUID(), sellerId);
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        Order result = service.getOrder(new GetOrderQuery(order.getId().value(), sellerId));
        assertThat(result).isSameAs(order);
    }

    @Test
    @DisplayName("buyer/seller 둘 다 아닌 사용자 → UnauthorizedOrderAccessException")
    void strangerForbidden() {
        UUID stranger = UUID.randomUUID();
        Order order = OrderTestFixtures.requestedOrder(UUID.randomUUID(), UUID.randomUUID());
        when(orderRepository.findByIdOrThrow(order.getId())).thenReturn(order);

        assertThatThrownBy(() ->
                service.getOrder(new GetOrderQuery(order.getId().value(), stranger))
        ).isInstanceOf(UnauthorizedOrderAccessException.class);
    }
}
