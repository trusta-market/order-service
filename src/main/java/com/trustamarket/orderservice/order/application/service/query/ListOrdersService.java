package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import com.trustamarket.orderservice.order.application.port.in.ListOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 전체 주문 목록 (ADMIN) — 권한 검증은 컨트롤러 @PreAuthorize 1차
@Service
@RequiredArgsConstructor
public class ListOrdersService implements ListOrdersUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryView> list(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderSummaryView::from);
    }
}
