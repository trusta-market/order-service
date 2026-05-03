package com.trustamarket.orderservice.order.domain.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

// product-service 조회 실패 (404 / 통신 장애 / 빈 응답) 통합 예외.
// 502 Bad Gateway — wallet 의 WalletCommunicationException 과 동일한 정책.
public class ProductLookupException extends OrderException {

    private static final String USER_MESSAGE = "상품 정보를 조회할 수 없습니다. 잠시 후 다시 시도해주세요.";

    public ProductLookupException(UUID productId) {
        super(HttpStatus.BAD_GATEWAY, USER_MESSAGE, "productId=" + productId);
    }

    public ProductLookupException(UUID productId, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, USER_MESSAGE, "productId=" + productId);
        initCause(cause);
    }
}
