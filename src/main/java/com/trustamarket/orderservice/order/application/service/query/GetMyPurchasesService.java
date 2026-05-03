package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.application.port.in.GetMyPurchasesUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// /api/v1/orders/me/purchases — actor 가 buyer 로 참여한 주문만
@Service
@RequiredArgsConstructor
public class GetMyPurchasesService implements GetMyPurchasesUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryView> getMyPurchases(GetMyPurchasesQuery query) {
        return orderRepository.findByBuyerId(query.buyerId(), query.pageable())
                .map(OrderSummaryView::from);
    }
}
