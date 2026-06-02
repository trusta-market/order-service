package com.trustamarket.orderservice.order.adapter.out.notification;

import com.trustamarket.common.response.CommonResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

// notification-service 의 AlertManager 포맷 webhook 호출용.
// reconciliation 의 GIVEN_UP 알림을 Discord 로 전달.
@FeignClient(name = "notification-service")
public interface NotificationFeignClient {

    @PostMapping("/api/v1/alerts/webhook")
    ResponseEntity<CommonResponse<Void>> send(@RequestBody AlertManagerWebhookPayload payload);

    // notification-service 의 AlertManagerWebhookRequest 와 동일 필드.
    record AlertManagerWebhookPayload(
            String version,
            String status,
            Map<String, String> commonLabels,
            List<AlertItem> alerts
    ) {
        public record AlertItem(
                String status,
                Map<String, String> labels,
                Map<String, String> annotations,
                OffsetDateTime startsAt
        ) {}
    }
}
