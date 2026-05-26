package com.trustamarket.orderservice.order.adapter.in.web;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.common.response.PagedResponse;
import com.trustamarket.common.util.SecurityUtil;
import com.trustamarket.orderservice.order.adapter.in.web.dto.response.OrderDetailResponse;
import com.trustamarket.orderservice.order.adapter.in.web.dto.response.OrderStatusHistoryResponse;
import com.trustamarket.orderservice.order.adapter.in.web.dto.response.OrderSummaryResponse;
import com.trustamarket.orderservice.order.application.port.in.GetMyOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetMyOrdersUseCase.GetMyOrdersQuery;
import com.trustamarket.orderservice.order.application.port.in.GetMyPurchasesUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetMyPurchasesUseCase.GetMyPurchasesQuery;
import com.trustamarket.orderservice.order.application.port.in.GetMySalesUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetMySalesUseCase.GetMySalesQuery;
import com.trustamarket.orderservice.order.application.port.in.GetOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetOrderUseCase.GetOrderQuery;
import com.trustamarket.orderservice.order.application.port.in.GetStatusHistoryUseCase;
import com.trustamarket.orderservice.order.application.port.in.ListOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.in.OrderSearchCriteria;
import com.trustamarket.orderservice.order.application.port.in.SearchOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.in.SearchOrdersUseCase.SearchOrdersQuery;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// 주문 Query — 본인 조회(MEMBER) + ADMIN 조회/검색/이력
// /api/v1/orders/... (member 본인) vs /api/v1/admin/orders/... (ADMIN) 분리
@RestController
@RequiredArgsConstructor
@RequestMapping
public class OrderQueryController {

    private final GetOrderUseCase getOrderUseCase;
    private final GetMyOrdersUseCase getMyOrdersUseCase;
    private final GetMyPurchasesUseCase getMyPurchasesUseCase;
    private final GetMySalesUseCase getMySalesUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final SearchOrdersUseCase searchOrdersUseCase;
    private final GetStatusHistoryUseCase getStatusHistoryUseCase;

    // 본인이 buyer 또는 seller로 참여한 모든 주문
    @GetMapping("/api/v1/orders/me")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getMyOrders(Pageable pageable) {
        UUID actorId = SecurityUtil.getCurrentUserIdOrThrow();
        Page<OrderSummaryResponse> page = getMyOrdersUseCase.getMyOrders(new GetMyOrdersQuery(actorId, pageable))
                .map(OrderSummaryResponse::from);
        return ResponseEntity.ok(PagedResponse.of(200, page));
    }

    @GetMapping("/api/v1/orders/me/purchases")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getMyPurchases(Pageable pageable) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        Page<OrderSummaryResponse> page = getMyPurchasesUseCase.getMyPurchases(new GetMyPurchasesQuery(buyerId, pageable))
                .map(OrderSummaryResponse::from);
        return ResponseEntity.ok(PagedResponse.of(200, page));
    }

    @GetMapping("/api/v1/orders/me/sales")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> getMySales(Pageable pageable) {
        UUID sellerId = SecurityUtil.getCurrentUserIdOrThrow();
        Page<OrderSummaryResponse> page = getMySalesUseCase.getMySales(new GetMySalesQuery(sellerId, pageable))
                .map(OrderSummaryResponse::from);
        return ResponseEntity.ok(PagedResponse.of(200, page));
    }

    // 단건 상세 — buyer 또는 seller 본인만 (application 레이어 OrderAccessGuard 가 검증)
    @GetMapping("/api/v1/orders/{orderId}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<CommonResponse<OrderDetailResponse>> getOrder(@PathVariable UUID orderId) {
        UUID actorId = SecurityUtil.getCurrentUserIdOrThrow();
        OrderDetailResponse result = OrderDetailResponse.from(
                getOrderUseCase.getOrder(new GetOrderQuery(orderId, actorId))
        );
        return ResponseEntity.ok(CommonResponse.of(200, result));
    }

    @GetMapping("/api/v1/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> list(Pageable pageable) {
        Page<OrderSummaryResponse> page = listOrdersUseCase.list(pageable).map(OrderSummaryResponse::from);
        return ResponseEntity.ok(PagedResponse.of(200, page));
    }

    // 검색 — status/기간/buyerName 부분일치 (모두 nullable)
    // criteria record 의 compact constructor 가 fromDate>toDate 차단 + buyerName blank 정규화
    @GetMapping("/api/v1/admin/orders/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<OrderSummaryResponse>> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) String buyerName,
            Pageable pageable
    ) {
        OrderSearchCriteria criteria = new OrderSearchCriteria(status, fromDate, toDate, buyerName);
        Page<OrderSummaryResponse> page = searchOrdersUseCase.search(new SearchOrdersQuery(criteria, pageable))
                .map(OrderSummaryResponse::from);
        return ResponseEntity.ok(PagedResponse.of(200, page));
    }

    @GetMapping("/api/v1/admin/orders/{orderId}/status-histories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResponse<List<OrderStatusHistoryResponse>>> getStatusHistory(@PathVariable UUID orderId) {
        List<OrderStatusHistoryResponse> list = getStatusHistoryUseCase.getHistory(orderId).stream()
                .map(OrderStatusHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(CommonResponse.of(200, list));
    }
}
