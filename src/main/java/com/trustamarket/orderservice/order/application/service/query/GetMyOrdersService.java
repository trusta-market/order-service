package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.application.port.in.GetMyOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// /api/v1/orders/me — actor 가 buyer 또는 seller 로 참여한 모든 주문 (통합)
// 권한: 자기 ID 로만 조회하므로 자동으로 본인 데이터만 반환됨
@Service
@RequiredArgsConstructor
public class GetMyOrdersService implements GetMyOrdersUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryView> getMyOrders(GetMyOrdersQuery query) {
        return orderRepository
                .findByBuyerIdOrSellerId(query.actorId(), query.actorId(), query.pageable())
                .map(OrderSummaryView::from);
    }
}
