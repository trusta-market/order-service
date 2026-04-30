package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidIdException;

import java.util.UUID;

// 주문 상태 전이 이력 — append-only 도메인 record
// Order 행위 메서드가 상태 전이를 일으킬 때마다 application service가 한 건 기록
// (audit/감사 추적용 — Order aggregate 내부 상태와는 별개 라이프사이클)
public record OrderStatusHistory(
        UUID id,
        OrderId orderId,
        OrderStatus prevStatus,
        OrderStatus nextStatus,
        Reason reason          // nullable — 취소/반송 같이 사유가 있는 전이만 채워짐
) {

    public OrderStatusHistory {
        if (id == null) throw new InvalidIdException("orderStatusHistoryId");
        if (orderId == null) throw new InvalidIdException("orderId");
        if (prevStatus == null) throw new InvalidIdException("prevStatus");
        if (nextStatus == null) throw new InvalidIdException("nextStatus");
    }

    // 새 history 생성
    public static OrderStatusHistory record(
            OrderId orderId,
            OrderStatus prevStatus,
            OrderStatus nextStatus,
            Reason reason
    ) {
        return new OrderStatusHistory(UUID.randomUUID(), orderId, prevStatus, nextStatus, reason);
    }

    // DB 복원
    public static OrderStatusHistory restore(
            UUID id,
            OrderId orderId,
            OrderStatus prevStatus,
            OrderStatus nextStatus,
            Reason reason
    ) {
        return new OrderStatusHistory(id, orderId, prevStatus, nextStatus, reason);
    }
}
