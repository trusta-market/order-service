package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.port.in.GetStatusHistoryUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderStatusHistoryRepository;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// 상태 이력 조회 (ADMIN) — created_at 오름차순 List 반환
@Service
@RequiredArgsConstructor
public class GetStatusHistoryService implements GetStatusHistoryUseCase {

    private final OrderStatusHistoryRepository historyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getHistory(UUID orderId) {
        return historyRepository.findByOrderId(OrderId.of(orderId));
    }
}
