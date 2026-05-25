package com.trustamarket.orderservice.order.adapter.out.wallet;

import com.trustamarket.common.response.CommonResponse;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointRequest;
import com.trustamarket.orderservice.order.application.port.out.WalletPaymentPort.DeductPointResponse;
import com.trustamarket.orderservice.order.domain.exception.WalletCommunicationException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletPaymentAdapterTest {

    @Mock WalletFeignClient walletFeignClient;
    @InjectMocks WalletPaymentAdapter adapter;

    @Test
    @DisplayName("정상 차감 — ResponseEntity<CommonResponse<T>> unwrap")
    void deduct_success() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        var walletResp = new WalletFeignClient.UseWalletResponse(9_500_000L, null);
        when(walletFeignClient.usePoint(any()))
                .thenReturn(ResponseEntity.ok(CommonResponse.of(200, walletResp)));

        DeductPointResponse result = adapter.deduct(new DeductPointRequest(orderId, buyerId, 500_000L));

        assertThat(result.balance()).isEqualTo(9_500_000L);
        assertThat(result.shortage()).isNull();
    }

    @Test
    @DisplayName("ResponseEntity 자체 null — WalletCommunicationException")
    void deduct_nullResponse() {
        when(walletFeignClient.usePoint(any()))
                .thenReturn(null);

        assertThatThrownBy(() -> adapter.deduct(new DeductPointRequest(UUID.randomUUID(), UUID.randomUUID(), 100L)))
                .isInstanceOf(WalletCommunicationException.class);
    }

    @Test
    @DisplayName("body null — WalletCommunicationException")
    void deduct_nullBody() {
        when(walletFeignClient.usePoint(any()))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> adapter.deduct(new DeductPointRequest(UUID.randomUUID(), UUID.randomUUID(), 100L)))
                .isInstanceOf(WalletCommunicationException.class);
    }

    @Test
    @DisplayName("data null — WalletCommunicationException")
    void deduct_nullData() {
        when(walletFeignClient.usePoint(any()))
                .thenReturn(ResponseEntity.ok(CommonResponse.of(200, null)));

        assertThatThrownBy(() -> adapter.deduct(new DeductPointRequest(UUID.randomUUID(), UUID.randomUUID(), 100L)))
                .isInstanceOf(WalletCommunicationException.class);
    }

    @Test
    @DisplayName("음수 balance — WalletCommunicationException")
    void deduct_negativeBalance() {
        var walletResp = new WalletFeignClient.UseWalletResponse(-100L, null);
        when(walletFeignClient.usePoint(any()))
                .thenReturn(ResponseEntity.ok(CommonResponse.of(200, walletResp)));

        assertThatThrownBy(() -> adapter.deduct(new DeductPointRequest(UUID.randomUUID(), UUID.randomUUID(), 100L)))
                .isInstanceOf(WalletCommunicationException.class);
    }

    @Test
    @DisplayName("음수 shortage — WalletCommunicationException")
    void deduct_negativeShortage() {
        var walletResp = new WalletFeignClient.UseWalletResponse(0L, -500L);
        when(walletFeignClient.usePoint(any()))
                .thenReturn(ResponseEntity.ok(CommonResponse.of(200, walletResp)));

        assertThatThrownBy(() -> adapter.deduct(new DeductPointRequest(UUID.randomUUID(), UUID.randomUUID(), 100L)))
                .isInstanceOf(WalletCommunicationException.class);
    }
}
