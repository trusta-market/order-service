package com.trustamarket.orderservice.order.adapter.in.web;

import com.trustamarket.common.config.security.UserDetailsImpl;
import com.trustamarket.common.util.SecurityUtil;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.CancelOrderRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.CreateOrderRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.DecideReturnRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.request.RequestReturnRequest;
import com.trustamarket.orderservice.order.adapter.in.web.dto.response.CreateOrderResponse;
import com.trustamarket.orderservice.order.application.port.in.CancelOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.ConfirmOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.ConfirmOrderUseCase.ConfirmOrderCommand;
import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase;
import com.trustamarket.orderservice.order.application.port.in.DecideReturnUseCase;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase;
import com.trustamarket.orderservice.order.application.port.in.RequestPaymentUseCase.RequestPaymentCommand;
import com.trustamarket.orderservice.order.application.port.in.RequestReturnUseCase;
import com.trustamarket.orderservice.order.domain.model.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// 주문 Command — 생성/결제/취소/확정/반송 요청 + ADMIN 반송 결정
// 인증: SecurityUtil 로 SecurityContext 의 UserDetailsImpl 추출 (Gateway X-User-* 헤더 → LoginFilter 가 주입)
// 권한: 메서드 단위 @PreAuthorize. ADMIN 액션은 /api/v1/admin/orders/... 경로로 분리
@RestController
@RequiredArgsConstructor
@RequestMapping
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final RequestPaymentUseCase requestPaymentUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final RequestReturnUseCase requestReturnUseCase;
    private final DecideReturnUseCase decideReturnUseCase;

    @PostMapping("/api/v1/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('MEMBER')")
    public CreateOrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        UserDetailsImpl me = currentUser();
        Order created = createOrderUseCase.createOrder(request.toCommand(me.getUuid(), me.getName()));
        return CreateOrderResponse.from(created);
    }

    @PostMapping("/api/v1/orders/{orderId}/payment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MEMBER')")
    public void requestPayment(@PathVariable UUID orderId) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        requestPaymentUseCase.requestPayment(new RequestPaymentCommand(orderId, buyerId));
    }

    @PostMapping("/api/v1/orders/{orderId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public void cancel(@PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
        UUID actorId = SecurityUtil.getCurrentUserIdOrThrow();
        cancelOrderUseCase.cancel(request.toCommand(orderId, actorId));
    }

    @PostMapping("/api/v1/orders/{orderId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MEMBER')")
    public void confirm(@PathVariable UUID orderId) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        confirmOrderUseCase.confirm(new ConfirmOrderCommand(orderId, buyerId));
    }

    @PostMapping("/api/v1/orders/{orderId}/return")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('MEMBER')")
    public void requestReturn(@PathVariable UUID orderId, @Valid @RequestBody RequestReturnRequest request) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        requestReturnUseCase.requestReturn(request.toCommand(orderId, buyerId));
    }

    @PostMapping("/api/v1/admin/orders/{orderId}/return/decision")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void decideReturn(@PathVariable UUID orderId, @Valid @RequestBody DecideReturnRequest request) {
        UUID adminId = SecurityUtil.getCurrentUserIdOrThrow();
        decideReturnUseCase.decide(request.toCommand(orderId, adminId));
    }

    // SecurityContext 에서 UserDetailsImpl 까지 직접 꺼내야 하는 경우 (createOrder 의 buyerName 등)
    private static UserDetailsImpl currentUser() {
        return SecurityUtil.getCurrentUser()
                .orElseThrow(() -> new com.trustamarket.common.exception.UnauthorizedException());
    }
}
