package com.trustamarket.orderservice.order.adapter.in.web;

import com.trustamarket.common.config.security.UserDetailsImpl;
import com.trustamarket.common.util.SecurityUtil;
import com.trustamarket.common.response.CommonResponse;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// 주문 Command — 생성/결제/취소/확정/반송 요청 + ADMIN 반송 결정
// 인증: SecurityUtil 로 SecurityContext 의 UserDetailsImpl 추출 (Gateway X-User-* 헤더 → LoginFilter 가 주입)
// 권한: 메서드 단위 @PreAuthorize. ADMIN 액션은 /api/v1/admin/orders/... 경로로 분리
@RestController
@RequiredArgsConstructor
@RequestMapping
@Validated
public class OrderCommandController {

    private final CreateOrderUseCase createOrderUseCase;
    private final RequestPaymentUseCase requestPaymentUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final RequestReturnUseCase requestReturnUseCase;
    private final DecideReturnUseCase decideReturnUseCase;

    @PostMapping("/api/v1/orders")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<CommonResponse<CreateOrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        UserDetailsImpl me = currentUser();
        CreateOrderResponse result = CreateOrderResponse.from(
                createOrderUseCase.createOrder(request.toCommand(me.getUuid(), me.getName()))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of(201, result));
    }

    @PostMapping("/api/v1/orders/{orderId}/payments")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> requestPayment(
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey
    ) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        requestPaymentUseCase.requestPayment(new RequestPaymentCommand(orderId, buyerId, idempotencyKey));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/orders/{orderId}/cancellations")
    @PreAuthorize("hasAnyRole('MEMBER','ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable UUID orderId, @Valid @RequestBody CancelOrderRequest request) {
        UUID actorId = SecurityUtil.getCurrentUserIdOrThrow();
        cancelOrderUseCase.cancel(request.toCommand(orderId, actorId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/orders/{orderId}/confirmations")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> confirm(@PathVariable UUID orderId) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        confirmOrderUseCase.confirm(new ConfirmOrderCommand(orderId, buyerId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/orders/{orderId}/returns")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> requestReturn(@PathVariable UUID orderId, @Valid @RequestBody RequestReturnRequest request) {
        UUID buyerId = SecurityUtil.getCurrentUserIdOrThrow();
        requestReturnUseCase.requestReturn(request.toCommand(orderId, buyerId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/orders/{orderId}/returns/decisions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> decideReturn(@PathVariable UUID orderId, @Valid @RequestBody DecideReturnRequest request) {
        UUID adminId = SecurityUtil.getCurrentUserIdOrThrow();
        decideReturnUseCase.decide(request.toCommand(orderId, adminId));
        return ResponseEntity.noContent().build();
    }

    // SecurityContext 에서 UserDetailsImpl 까지 직접 꺼내야 하는 경우 (createOrder 의 buyerName 등)
    private static UserDetailsImpl currentUser() {
        return SecurityUtil.getCurrentUser()
                .orElseThrow(() -> new com.trustamarket.common.exception.UnauthorizedException());
    }
}
