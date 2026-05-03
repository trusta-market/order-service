package com.trustamarket.orderservice.order.adapter.in.web.dto.request;

import com.trustamarket.orderservice.order.application.port.in.CreateOrderUseCase.CreateOrderCommand;
import com.trustamarket.orderservice.order.domain.model.OrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// POST /api/v1/orders 요청 본문
// buyerId/buyerName 은 Authorization 헤더 인증 사용자에서 채워지므로 본문에 받지 않음
public record CreateOrderRequest(
        @NotNull UUID sellerId,
        @NotBlank String sellerName,
        @NotNull UUID productId,
        @NotBlank String productName,
        @Min(0) long productPrice,
        @NotNull OrderType type,
        @Min(0) long shippingFee
) {
    public CreateOrderCommand toCommand(UUID buyerId, String buyerName) {
        return new CreateOrderCommand(
                buyerId, buyerName,
                sellerId, sellerName,
                productId, productName, productPrice,
                type,
                shippingFee
        );
    }
}
