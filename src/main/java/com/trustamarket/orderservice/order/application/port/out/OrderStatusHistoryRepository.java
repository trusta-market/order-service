package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;

import java.util.List;

// 상태 이력 영속화 port
// append-only — save/조회만 (update/delete 없음)
public interface OrderStatusHistoryRepository {

    OrderStatusHistory save(OrderStatusHistory history);

    List<OrderStatusHistory> findByOrderId(OrderId orderId);
}
