package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderDetailView;
import com.trustamarket.orderservice.order.application.port.in.GetOrderUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderAccessGuard;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 주문 단건 조회 — GET /api/orders/{id}
// 권한: 컨트롤러 @PreAuthorize MEMBER 1차 + service에서 buyer/seller 본인 검증
// ADMIN이 다른 사용자 주문을 봐야 하면 ListOrders/SearchOrders로 우회 (별도 endpoint X)
@Service
@RequiredArgsConstructor
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public OrderDetailView getOrder(GetOrderQuery query) {
        // 1. 조회 — 없으면 OrderNotFoundException
        Order order = orderRepository.findByIdOrThrow(OrderId.of(query.orderId()));
        // 2. 권한 검증 — buyer 또는 seller 본인이어야 함. 아니면 UnauthorizedOrderAccessException
        OrderAccessGuard.verifyBuyerOrSeller(order, query.actorId());
        return OrderDetailView.from(order);
    }
}
