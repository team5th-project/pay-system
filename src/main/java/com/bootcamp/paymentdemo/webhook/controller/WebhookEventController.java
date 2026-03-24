package com.bootcamp.paymentdemo.webhook.controller;

import com.bootcamp.paymentdemo.webhook.dto.PortOneWebhookRequest;
import com.bootcamp.paymentdemo.webhook.service.WebhookEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/portone")
@RequiredArgsConstructor
public class WebhookEventController {
    private final WebhookEventService webhookEventService;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String signature,
            @RequestHeader("webhook-timestamp") String timestamp,
            @RequestBody String rawPayload) {
        webhookEventService.handleWebhook(webhookId, signature, timestamp, rawPayload);
        return ResponseEntity.ok().build();
    }
}
