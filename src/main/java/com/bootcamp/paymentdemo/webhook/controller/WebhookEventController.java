package com.bootcamp.paymentdemo.webhook.controller;


import com.bootcamp.paymentdemo.webhook.service.WebhookEventService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/portone")
@RequiredArgsConstructor
@Slf4j
public class WebhookEventController {
//    private final WebhookEventService webhookEventService;
    private final WebhookEventService2 webhookEventService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String signature,
            @RequestHeader("webhook-timestamp") String timestamp,
            @RequestBody String rawPayload) {

        log.info("PortOne 웹훅 수신 - webhookId={}, timestamp = {}", webhookId, timestamp);
        // 실제 운영 시에 rawPayload랑 timestamp는 없애기
        log.info("PortOne 웹훅 수신 - 데이터 : {}", rawPayload);

        webhookEventService.handleWebhook(webhookId, signature, timestamp, rawPayload);

        return ResponseEntity.ok().build();
    }
}
