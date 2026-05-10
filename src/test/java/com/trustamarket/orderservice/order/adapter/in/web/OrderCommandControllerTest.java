package com.trustamarket.orderservice.order.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustamarket.common.config.security.SecurityConfig;
import com.trustamarket.common.exception.GlobalExceptionAdvice;
import com.trustamarket.common.response.CommonResponseAdvice;
import com.trustamarket.orderservice.config.web.SecurityExceptionAdvice;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.CancelOrderRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.CreateOrderRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.DecideReturnRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.RequestReturnRequest;
import com.trustamarket.orderservice.order.application.port.in.CancelOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.ConfirmOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase;
import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase.Decision;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase;
import com.trustamarket.orderservice.order.application.port.in.RequestReturnUseCase;
import com.trustamarket.orderservice.order.application.dto.result.CreateOrderResult;
import com.trustamarket.orderservice.order.application.service.OrderTestFixtures;
import com.trustamarket.orderservice.order.domain.model.Order;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderCommandController.class)
@Import({SecurityConfig.class, GlobalExceptionAdvice.class, CommonResponseAdvice.class,
        SecurityExceptionAdvice.class, WebSliceTestConfig.class})
class OrderCommandControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CreateOrderUseCase createOrderUseCase;
    @MockBean RequestPaymentUseCase requestPaymentUseCase;
    @MockBean CancelOrderUseCase cancelOrderUseCase;
    @MockBean ConfirmOrderUseCase confirmOrderUseCase;
    @MockBean RequestReturnUseCase requestReturnUseCase;
    @MockBean DecideReturnUseCase decideReturnUseCase;

    private final UUID buyerId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    // ─── POST /api/v1/orders ───

    @Test
    @DisplayName("create — 201 + body")
    void create_happyPath() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                sellerId, "판매자",
                UUID.randomUUID(), "상품", 100_000L,
                OrderType.LOW, 3_000L);
        Order saved = OrderTestFixtures.requestedOrder(buyerId, sellerId);
        when(createOrderUseCase.createOrder(any())).thenReturn(CreateOrderResult.from(saved));

        mockMvc.perform(post("/api/v1/orders")
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.orderId").exists());
    }

    @Test
    @DisplayName("create — 인증 없음 → 401")
    void create_unauthenticated() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                sellerId, "판매자",
                UUID.randomUUID(), "상품", 100_000L,
                OrderType.LOW, 3_000L);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("create — ADMIN 권한으로는 거부 (MEMBER 전용) → 403")
    void create_forbiddenForAdmin() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                sellerId, "판매자",
                UUID.randomUUID(), "상품", 100_000L,
                OrderType.LOW, 3_000L);

        mockMvc.perform(post("/api/v1/orders")
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("create — sellerId 누락 → 400")
    void create_validationFailure() throws Exception {
        // sellerId null
        String invalidJson = """
                {
                  "sellerName": "판매자",
                  "productId": "%s",
                  "productName": "상품",
                  "productPrice": 100000,
                  "type": "LOW",
                  "shippingFee": 3000
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/v1/orders/{id}/payments ───

    @Test
    @DisplayName("requestPayment — 204")
    void requestPayment_happyPath() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
        verify(requestPaymentUseCase).requestPayment(any());
    }

    @Test
    @DisplayName("requestPayment — 인증 없음 → 401")
    void requestPayment_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .with(csrf())
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("requestPayment — ADMIN 거부 → 403")
    void requestPayment_forbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID())))
                        .with(csrf())
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }


    // ─── POST /api/v1/orders/{id}/cancellations ───

    @Test
    @DisplayName("cancel — 204 (MEMBER)")
    void cancel_happyPath_member() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/cancellations", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("변심"))))
                .andExpect(status().isNoContent());
        verify(cancelOrderUseCase).cancel(any());
    }

    @Test
    @DisplayName("cancel — 204 (ADMIN 도 허용)")
    void cancel_happyPath_admin() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/cancellations", orderId)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("관리자 직권"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("cancel — reason blank → 400")
    void cancel_blankReason() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/cancellations", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("cancel — 인증 없음 → 401")
    void cancel_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/cancellations", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("변심"))))
                .andExpect(status().isUnauthorized());
    }

    // ─── POST /api/v1/orders/{id}/confirmations ───

    @Test
    @DisplayName("confirm — 204")
    void confirm_happyPath() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/confirmations", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(confirmOrderUseCase).confirm(any());
    }

    @Test
    @DisplayName("confirm — 인증 없음 → 401")
    void confirm_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/confirmations", orderId)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("confirm — ADMIN 거부 → 403")
    void confirm_forbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/confirmations", orderId)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID())))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/v1/orders/{id}/returns ───

    @Test
    @DisplayName("requestReturn — 204")
    void requestReturn_happyPath() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/returns", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestReturnRequest("불량"))))
                .andExpect(status().isNoContent());
        verify(requestReturnUseCase).requestReturn(any());
    }

    @Test
    @DisplayName("requestReturn — 인증 없음 → 401")
    void requestReturn_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/returns", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestReturnRequest("불량"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("requestReturn — reason blank → 400")
    void requestReturn_blankReason() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/returns", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/v1/admin/orders/{id}/returns/decisions ───

    @Test
    @DisplayName("decideReturn — 204 (ADMIN, APPROVE)")
    void decideReturn_happyPath_approve() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/returns/decisions", orderId)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DecideReturnRequest(Decision.APPROVE, null))))
                .andExpect(status().isNoContent());
        verify(decideReturnUseCase).decide(any());
    }

    @Test
    @DisplayName("decideReturn — 204 (ADMIN, REJECT + reason)")
    void decideReturn_happyPath_reject() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/returns/decisions", orderId)
                        .with(authentication(TestAuth.adminAuth(UUID.randomUUID())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DecideReturnRequest(Decision.REJECT, "사유"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("decideReturn — MEMBER 거부 → 403")
    void decideReturn_forbiddenForMember() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/returns/decisions", orderId)
                        .with(authentication(TestAuth.memberAuth(buyerId, "구매자")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DecideReturnRequest(Decision.APPROVE, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("decideReturn — 인증 없음 → 401")
    void decideReturn_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/returns/decisions", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DecideReturnRequest(Decision.APPROVE, null))))
                .andExpect(status().isUnauthorized());
    }
}
