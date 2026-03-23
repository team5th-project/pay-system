package com.bootcamp.paymentdemo.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// PortOne이 우리 서버에 보낸 HTTP 요청 body를 담는 DTO
public record PortOneWebhookRequest(
        @NotBlank
        String type,
        @NotNull
        Data data
) {
    public record Data(
            @NotBlank
            String paymentId) {
    }
}
