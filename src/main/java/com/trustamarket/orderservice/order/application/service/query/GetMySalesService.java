package com.trustamarket.orderservice.order.application.service.query;

import com.trustamarket.orderservice.order.application.port.in.GetMySalesUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// /orders/me/sales — actor가 seller로 참여한 주문만
@Service
@RequiredArgsConstructor
public class GetMySalesService implements GetMySalesUseCase {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getMySales(GetMySalesQuery query) {
        return orderRepository.findBySellerId(query.sellerId(), query.pageable());
    }
}
