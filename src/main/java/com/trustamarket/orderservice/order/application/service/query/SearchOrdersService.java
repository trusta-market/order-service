package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.application.port.in.SearchOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 주문 검색 (ADMIN) — Specification 동적 쿼리로 조건 조합
@Service
@RequiredArgsConstructor
public class SearchOrdersService implements SearchOrdersUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryView> search(SearchOrdersQuery query) {
        return orderRepository.search(query.criteria(), query.pageable())
                .map(OrderSummaryView::from);
    }
}
