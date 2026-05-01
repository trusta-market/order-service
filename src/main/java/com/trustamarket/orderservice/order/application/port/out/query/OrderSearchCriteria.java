package com.trustamarket.orderservice.order.application.port.out.query;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;

import java.time.Instant;

// SearchOrdersUseCase 검색 조건 (ADMIN)
// 모든 필드 nullable — null이면 해당 조건 무시
// MVP scope: status + 기간(fromDate~toDate) + buyerName 부분일치
// TODO: 추후 풍부한 검색(키워드 full-text + 다중 필터) 도입 시 확장
public record OrderSearchCriteria(
        OrderStatus status,        // 정확히 일치 (REQUESTED, PAID 등)
        Instant fromDate,          // createdAt >= fromDate
        Instant toDate,            // createdAt <= toDate
        String buyerName           // buyer_name LIKE 연산 '%X%'
) {
    public OrderSearchCriteria {
        // 기간 역전 차단
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidIdException("dateRange");   // TODO: 시각 범위 전용 예외 신설 검토
        }
        // buyerName blank → null 정규화 (조건 무시 의도와 일관)
        if (buyerName != null && buyerName.isBlank()) {
            buyerName = null;
        }
    }
}
