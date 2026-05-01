package com.trustamarket.orderservice.order.application.service.command;

import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase;
import com.trustamarket.orderservice.order.application.port.out.OrderRepository;
import com.trustamarket.orderservice.order.application.service.support.OrderHistoryRecorder;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.Reason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// RETURN_REQUESTED → RETURN_APPROVED 또는 RETURN_REJECTED (ADMIN 전용)
// 권한 검증은 컨트롤러 @PreAuthorize에서 1차, 본 service는 도메인 행위만
@Service
@RequiredArgsConstructor
public class DecideReturnService implements DecideReturnUseCase {

    private final OrderRepository orderRepository;
    private final OrderHistoryRecorder historyRecorder;

    @Override
    @Transactional
    public void decide(DecideReturnCommand cmd) {
        Order order = orderRepository.findByIdOrThrow(OrderId.of(cmd.orderId()));

        OrderStatus pre = order.getStatus();
        Reason rejectReason = null;
        if (cmd.decision() == Decision.APPROVE) {
            order.approveReturn();
        } else {
            rejectReason = Reason.of(cmd.rejectReason());
            order.rejectReturn(rejectReason);
        }
        historyRecorder.record(order.getId(), pre, order.getStatus(), rejectReason);

        orderRepository.save(order);
    }
}
