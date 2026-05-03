package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.application.port.in.GetMySalesUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// /api/v1/orders/me/sales — actor 가 seller 로 참여한 주문만
@Service
@RequiredArgsConstructor
public class GetMySalesService implements GetMySalesUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryView> getMySales(GetMySalesQuery query) {
        return orderRepository.findBySellerId(query.sellerId(), query.pageable())
                .map(OrderSummaryView::from);
    }
}
