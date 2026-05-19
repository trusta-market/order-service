package com.trustamarket.orderservice.order.application.port.out;

import com.trustamarket.orderservice.order.application.port.in.OrderSearchCriteria;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

// Order 영속화 port (out) — 단순화 위해 read/write 모두 한 인터페이스에
// TODO: command/query 책임 분리 가치 생기면 OrderQueryRepository 별도 분리 검토 (현재는 ROI 작아 통합)
public interface OrderRepository {

    // Command

    // 기존 entity 업데이트 (merge — JPA 가 SELECT 후 변경 여부 판단 → UPDATE 또는 INSERT).
    // findById 로 가져온 managed entity 갱신에 사용.
    Order save(Order order);

    // 신규 entity 명시적 INSERT (persist — SELECT skip).
    // PK 가 도메인에서 미리 생성 (OrderId.generate) 되는 케이스만 안전.
    // 사용처: CreateOrderService 의 신규 주문 INSERT.
    Order saveNew(Order order);

    Optional<Order> findById(OrderId id);

    Order findByIdOrThrow(OrderId id);

    // Query
    Page<Order> findByBuyerId(UUID buyerId, Pageable pageable);

    Page<Order> findBySellerId(UUID sellerId, Pageable pageable);

    // /orders/me 통합 조회 — buyer 또는 seller 둘 중 하나라도 본인이면 포함
    Page<Order> findByBuyerIdOrSellerId(UUID buyerId, UUID sellerId, Pageable pageable);

    Page<Order> findAll(Pageable pageable);

    Page<Order> search(OrderSearchCriteria criteria, Pageable pageable);
}
