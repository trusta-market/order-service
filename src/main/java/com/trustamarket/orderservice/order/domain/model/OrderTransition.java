package com.trustamarket.orderservice.order.domain.model;

import com.trustamarket.orderservice.order.domain.exception.InvalidStatusTransitionException;

import java.util.Map;

import static com.trustamarket.orderservice.order.domain.model.OrderAction.APPROVE_RETURN;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.CANCEL;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.CONFIRM;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.MARK_DELIVERED;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.MARK_PAID;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.MARK_REFUNDED;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.REJECT_RETURN;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.REQUEST_PAYMENT;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.REQUEST_RETURN;
import static com.trustamarket.orderservice.order.domain.model.OrderAction.START_SHIPPING;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.CANCELLED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.CONFIRMED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.DELIVERED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.PAID;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.PAYMENT_PENDING;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.REFUND_COMPLETED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.REFUND_PROCESSING;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.REQUESTED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.RETURN_APPROVED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.RETURN_REJECTED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.RETURN_REQUESTED;
import static com.trustamarket.orderservice.order.domain.model.OrderStatus.SHIPPING;

// Order 상태 머신 — 표 기반
// 모든 상태 전이는 (현재 상태 + 액션) -> 다음 상태 형태로 TABLE에 정의
// 표에 없는 조합은 InvalidStatusTransitionException throw
//
// 시나리오 매트릭스가 코드에 1:1 매칭되어 한 곳에서 전이 규칙 관리.
public final class OrderTransition {

    public record Key(OrderStatus from, OrderAction action) {}

    private static final Map<Key, OrderStatus> TABLE = Map.ofEntries(
            // Happy path — REQUESTED부터 CONFIRMED까지. CONFIRMED가 거래 종결
            // Map.entry((현재상태+액션) -> 다음 상태)
            Map.entry(new Key(REQUESTED,             REQUEST_PAYMENT),  PAYMENT_PENDING),
            Map.entry(new Key(PAYMENT_PENDING,       MARK_PAID),        PAID),
            Map.entry(new Key(PAID,                  START_SHIPPING),   SHIPPING),
            Map.entry(new Key(SHIPPING,              MARK_DELIVERED),   DELIVERED),
            Map.entry(new Key(DELIVERED,             CONFIRM),          CONFIRMED),

            // 취소 분기 (배송 시작 전까지만)
            Map.entry(new Key(REQUESTED,             CANCEL),           CANCELLED),
            Map.entry(new Key(PAYMENT_PENDING,       CANCEL),           CANCELLED),
            Map.entry(new Key(PAID,                  CANCEL),           REFUND_PROCESSING),

            // 환불 완료 (Wallet RefundCompleted 수신)
            Map.entry(new Key(REFUND_PROCESSING,     MARK_REFUNDED),    REFUND_COMPLETED),

            // 반송 분기 (배송 시작 후만)
            Map.entry(new Key(SHIPPING,              REQUEST_RETURN),   RETURN_REQUESTED),
            Map.entry(new Key(DELIVERED,             REQUEST_RETURN),   RETURN_REQUESTED),
            Map.entry(new Key(RETURN_REQUESTED,      APPROVE_RETURN),   RETURN_APPROVED),
            Map.entry(new Key(RETURN_REQUESTED,      REJECT_RETURN),    RETURN_REJECTED)
    );

    // 현재 상태에 액션을 적용해 다음 상태 반환 (조합이 맞나 확인)
    // 허용되지 않는 (상태, 액션) 조합이면 InvalidStatusTransitionException throw
    public static OrderStatus apply(OrderStatus current, OrderAction action) {
        OrderStatus next = TABLE.get(new Key(current, action));
        if (next == null) {
            throw new InvalidStatusTransitionException(current, action);
        }
        return next;
    }

    // 정적 유틸 — 인스턴스화 차단
    private OrderTransition() {}
}
