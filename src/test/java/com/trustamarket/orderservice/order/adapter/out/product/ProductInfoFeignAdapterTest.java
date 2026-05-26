package com.trustamarket.orderservice.order.adapter.out.product;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.orderservice.order.application.port.out.ProductInfoPort.ProductInfo;
import com.trustamarket.orderservice.order.domain.exception.ProductLookupException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductInfoFeignAdapterTest {

    @Mock ProductFeignClient feignClient;
    @InjectMocks ProductInfoFeignAdapter adapter;

    @Test
    @DisplayName("정상 응답 — ResponseEntity<CommonResponse<T>> unwrap")
    void fetch_success() {
        UUID productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        var feignResp = new ProductFeignClient.ProductInfoFeignResponse(
                productId, sellerId, "테스트 상품", 100_000L, "ON_SALE");
        when(feignClient.getProductInfo(productId))
                .thenReturn(ResponseEntity.ok(CommonResponse.of(200, feignResp)));

        ProductInfo result = adapter.fetch(productId);

        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.sellerId()).isEqualTo(sellerId);
        assertThat(result.name()).isEqualTo("테스트 상품");
        assertThat(result.price()).isEqualTo(100_000L);
        assertThat(result.status()).isEqualTo("ON_SALE");
    }

    @Test
    @DisplayName("ResponseEntity 자체 null — ProductLookupException")
    void fetch_nullResponse() {
        UUID productId = UUID.randomUUID();
        when(feignClient.getProductInfo(productId))
                .thenReturn(null);

        assertThatThrownBy(() -> adapter.fetch(productId))
                .isInstanceOf(ProductLookupException.class);
    }

    @Test
    @DisplayName("body null — ProductLookupException")
    void fetch_nullBody() {
        UUID productId = UUID.randomUUID();
        when(feignClient.getProductInfo(productId))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> adapter.fetch(productId))
                .isInstanceOf(ProductLookupException.class);
    }

    @Test
    @DisplayName("data null — ProductLookupException")
    void fetch_nullData() {
        UUID productId = UUID.randomUUID();
        when(feignClient.getProductInfo(productId))
                .thenReturn(ResponseEntity.ok(CommonResponse.of(200, null)));

        assertThatThrownBy(() -> adapter.fetch(productId))
                .isInstanceOf(ProductLookupException.class);
    }
}
