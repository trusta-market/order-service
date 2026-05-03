package com.trustamarket.orderservice.order.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.common.config.security.SecurityConfig;
import com.trustamarket.common.exception.GlobalExceptionAdvice;
import com.trustamarket.common.response.CommonResponseAdvice;
import com.trustamarket.orderservice.config.web.SecurityExceptionAdvice;
import com.trustamarket.orderservice.order.application.port.in.GetMyOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetMyPurchasesUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetMySalesUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.GetStatusHistoryUseCase;
import com.trustamarket.orderservice.order.application.port.in.ListOrdersUseCase;
import com.trustamarket.orderservice.order.application.port.in.SearchOrdersUseCase;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderId;
import com.trustamarket.orderservice.order.domain.model.OrderStatus;
import com.trustamarket.orderservice.order.domain.model.OrderStatusHistory;
import com.trustamarket.orderservice.order.domain.model.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderQueryController.class)
@Import({SecurityConfig.class, GlobalExceptionAdvice.class, CommonResponseAdvice.class,
        SecurityExceptionAdvice.class, WebSliceTestConfig.class})
class OrderQueryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean GetOrderUseCase getOrderUseCase;
    @MockBean GetMyOrdersUseCase getMyOrdersUseCase;
    @MockBean GetMyPurchasesUseCase getMyPurchasesUseCase;
    @MockBean GetMySalesUseCase getMySalesUseCase;
    @MockBean ListOrdersUseCase listOrdersUseCase;
    @MockBean SearchOrdersUseCase searchOrdersUseCase;
    @MockBean GetStatusHistoryUseCase getStatusHistoryUseCase;

    private final UUID memberUuid = UUID.randomUUID();
    private final UUID sellerUuid = UUID.randomUUID();
    private final UUID orderUuid = UUID.randomUUID();

    private Page<Order> singleOrderPage() {
        Order o = OrderTestFixtures.requestedOrder(memberUuid, sellerUuid);
        return new PageImpl<>(List.of(o), PageRequest.of(0, 20), 1);
    }

    // ─── GET /api/v1/orders/me ───

    @Test
    @DisplayName("getMyOrders — 200 (MEMBER)")
    void getMyOrders_happyPath() throws Exception {
        when(getMyOrdersUseCase.getMyOrders(any())).thenReturn(singleOrderPage());

        mockMvc.perform(get("/api/v1/orders/me")
                        .with(authentication(TestAuth.memberAuth(memberUuid, "구매자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pageInfo").exists());
    }

    @Test
    @DisplayName("getMyOrders — 인증 없음 → 401")
    void getMyOrders_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getMyOrders — ADMIN 거부 → 403")
    void getMyOrders_forbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me")
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/v1/orders/me/purchases ───

    @Test
    @DisplayName("getMyPurchases — 200")
    void getMyPurchases_happyPath() throws Exception {
        when(getMyPurchasesUseCase.getMyPurchases(any())).thenReturn(singleOrderPage());

        mockMvc.perform(get("/api/v1/orders/me/purchases")
                        .with(authentication(TestAuth.memberAuth(memberUuid, "구매자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("getMyPurchases — 인증 없음 → 401")
    void getMyPurchases_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me/purchases"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /api/v1/orders/me/sales ───

    @Test
    @DisplayName("getMySales — 200")
    void getMySales_happyPath() throws Exception {
        when(getMySalesUseCase.getMySales(any())).thenReturn(singleOrderPage());

        mockMvc.perform(get("/api/v1/orders/me/sales")
                        .with(authentication(TestAuth.memberAuth(sellerUuid, "판매자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("getMySales — 인증 없음 → 401")
    void getMySales_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me/sales"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /api/v1/orders/{id} ───

    @Test
    @DisplayName("getOrder — 200 + 상세 필드")
    void getOrder_happyPath() throws Exception {
        Order order = OrderTestFixtures.requestedOrder(memberUuid, sellerUuid);
        when(getOrderUseCase.getOrder(any())).thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/{id}", orderUuid)
                        .with(authentication(TestAuth.memberAuth(memberUuid, "구매자"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").exists())
                .andExpect(jsonPath("$.data.buyerId").exists())
                .andExpect(jsonPath("$.data.sellerId").exists())
                .andExpect(jsonPath("$.data.totalAmount").exists());
    }

    @Test
    @DisplayName("getOrder — 인증 없음 → 401")
    void getOrder_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderUuid))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getOrder — ADMIN 거부 → 403")
    void getOrder_forbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderUuid)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/v1/admin/orders ───

    @Test
    @DisplayName("list — 200 (ADMIN)")
    void list_happyPath() throws Exception {
        when(listOrdersUseCase.list(any())).thenReturn(singleOrderPage());

        mockMvc.perform(get("/api/v1/admin/orders")
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pageInfo").exists());
    }

    @Test
    @DisplayName("list — MEMBER 거부 → 403")
    void list_forbiddenForMember() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .with(authentication(TestAuth.memberAuth(memberUuid, "구매자"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("list — 인증 없음 → 401")
    void list_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    // ─── GET /api/v1/admin/orders/search ───

    @Test
    @DisplayName("search — 200 (ADMIN, 모든 파라미터 nullable)")
    void search_happyPath() throws Exception {
        when(searchOrdersUseCase.search(any())).thenReturn(singleOrderPage());

        mockMvc.perform(get("/api/v1/admin/orders/search")
                        .param("status", "REQUESTED")
                        .param("buyerName", "홍길동")
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("search — 파라미터 없이도 200")
    void search_noParams() throws Exception {
        when(searchOrdersUseCase.search(any())).thenReturn(singleOrderPage());

        mockMvc.perform(get("/api/v1/admin/orders/search")
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID()))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("search — MEMBER 거부 → 403")
    void search_forbiddenForMember() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/search")
                        .with(authentication(TestAuth.memberAuth(memberUuid, "구매자"))))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/v1/admin/orders/{id}/status-history ───

    @Test
    @DisplayName("getStatusHistory — 200 (ADMIN)")
    void getStatusHistory_happyPath() throws Exception {
        OrderStatusHistory h = OrderStatusHistory.record(
                OrderId.of(orderUuid),
                null,
                OrderStatus.REQUESTED,
                Reason.of("최초 생성"));
        when(getStatusHistoryUseCase.getHistory(orderUuid)).thenReturn(List.of(h));

        mockMvc.perform(get("/api/v1/admin/orders/{id}/status-history", orderUuid)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nextStatus").value("REQUESTED"));
    }

    @Test
    @DisplayName("getStatusHistory — MEMBER 거부 → 403")
    void getStatusHistory_forbiddenForMember() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/{id}/status-history", orderUuid)
                        .with(authentication(TestAuth.memberAuth(memberUuid, "구매자"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("getStatusHistory — 인증 없음 → 401")
    void getStatusHistory_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/{id}/status-history", orderUuid))
                .andExpect(status().isUnauthorized());
    }
}
