package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.application.port.out.query.OrderSearchCriteria;
import com.trustamarket.orderservice.order.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 주문 검색 — GET /api/orders/search (ADMIN)
// MVP: status + 기간(fromDate~toDate) + buyerName 부분일치
// TODO: 풍부한 검색 (키워드 full-text + 다중 필터) 도입 시 OrderSearchCriteria 확장
public interface SearchOrdersUseCase {

    Page<Order> search(SearchOrdersQuery query);

    record SearchOrdersQuery(
            OrderSearchCriteria criteria,
            Pageable pageable
    ) {}
}
