package com.trustamarket.orderservice.order.application.port.in;

import com.trustamarket.orderservice.order.application.dto.result.OrderSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 전체 주문 목록 조회 — GET /api/v1/admin/orders (ADMIN)
// 권한 검증은 컨트롤러 @PreAuthorize 에서 1차, service 는 단순 조회
public interface ListOrdersUseCase {

    Page<OrderSummaryView> list(Pageable pageable);
}
