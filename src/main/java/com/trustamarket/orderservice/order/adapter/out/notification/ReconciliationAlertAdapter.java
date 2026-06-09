package com.trustamarket.orderservice.order.adapter.out.notification;

import com.trustamarket.orderservice.order.application.port.out.ReconciliationAlertPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// reconciliation 의 GIVEN_UP 알림을 notification-service 의 AlertManager webhook 으로 전달.
// Discord 발송은 notification-service 내부 strategy 가 처리.
// 발송 실패는 caller (Processor) 가 catch — best-effort.
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationAlertAdapter implements ReconciliationAlertPort {

    private static final String ALERT_NAME = "payment-reconciliation-given-up";
    private static final String SEVERITY = "critical";

    private final NotificationFeignClient feignClient;

    @Override
    public void notifyGivenUp(UUID orderId, int retryCount, String lastError) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("alertname", ALERT_NAME);
        labels.put("severity", SEVERITY);
        labels.put("service", "order-service");
        labels.put("orderId", orderId.toString());
        labels.put("retryCount", Integer.toString(retryCount));

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("summary", "결제 결과 확인 재시도 한도 도달 — 수동 처리 필요");
        annotations.put("description", lastError != null ? lastError : "");

        var item = new NotificationFeignClient.AlertManagerWebhookPayload.AlertItem(
                "firing",
                labels,
                annotations,
                OffsetDateTime.now()
        );
        var payload = new NotificationFeignClient.AlertManagerWebhookPayload(
                "4",
                "firing",
                Map.of("severity", SEVERITY),
                List.of(item)
        );
        feignClient.send(payload);
        log.info("[ReconciliationAlert] GIVEN_UP 알림 발송 — orderId={}, retryCount={}",
                orderId, retryCount);
    }
}
