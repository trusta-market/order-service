package com.trustamarket.orderservice.order.application.service.support;

import com.trustamarket.orderservice.order.application.port.out.OrderStatusHistoryRepository;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import com.trustamarket.orderservice.order.domain.model.Reason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Order 상태 전이 history 기록 헬퍼 — 모든 Command service가 주입해서 사용
// 도메인 행위 메서드 호출 후 호출 → trace/감사용 이력 한 건 INSERT
// MANDATORY 전파 — 호출자(=Command service)가 트랜잭션을 열지 않으면 즉시 실패
// → 상태 변경과 history 기록의 원자성 보장 (한 트랜잭션 안에서 commit/rollback)
@Component
@RequiredArgsConstructor
public class OrderHistoryRecorder {

    private final OrderStatusHistoryRepository historyRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(OrderId orderId, OrderStatus prev, OrderStatus next, Reason reason) {
        historyRepository.save(OrderStatusHistory.record(orderId, prev, next, reason));
    }
}
