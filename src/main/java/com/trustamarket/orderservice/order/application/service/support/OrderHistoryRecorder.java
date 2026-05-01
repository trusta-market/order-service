package com.trustamarket.orderservice.order.application.service.support;

import com.trustamarket.orderservice.order.application.port.out.OrderStatusHistoryRepository;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import com.trustamarket.orderservice.order.domain.model.Reason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Order 상태 전이 history 기록 헬퍼 — 모든 Command service가 주입해서 사용
// 도메인 행위 메서드 호출 후 호출 → trace/감사용 이력 한 건 INSERT
@Component
@RequiredArgsConstructor
public class OrderHistoryRecorder {

    private final OrderStatusHistoryRepository historyRepository;

    public void record(OrderId orderId, OrderStatus prev, OrderStatus next, Reason reason) {
        historyRepository.save(OrderStatusHistory.record(orderId, prev, next, reason));
    }
}
