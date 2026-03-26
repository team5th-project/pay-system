package com.bootcamp.paymentdemo.webhook.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.webhook.dto.PortOneWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortOneWebhookVerifier {

    private final ObjectMapper objectMapper;

    @Value("${portone.webhook.secret}")
    private String webhookSecret;

    public PortOneWebhookRequest verifyAndParse(
            String webhookId,
            String signature,
            String timestamp,
            String rawPayload
    ) {
        validateHeaders(webhookId, signature, timestamp);
        validateTimestamp(timestamp);
        validateSignature(webhookId, signature, timestamp, rawPayload);

        try {
            return objectMapper.readValue(rawPayload, PortOneWebhookRequest.class);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
    }

    private void validateHeaders(String webhookId, String signature, String timestamp) {
        if (!StringUtils.hasText(webhookId) ||
                !StringUtils.hasText(signature) ||
                !StringUtils.hasText(timestamp)) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_HEADER);
        }
    }

    private void validateTimestamp(String timestamp) {
        long epochSeconds;

        try {
            epochSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
        }

        Instant requestTime = Instant.ofEpochSecond(epochSeconds);
        Instant now = Instant.now();

        long diff = Math.abs(Duration.between(requestTime, now).toMinutes());
        if (diff > 5) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
        }
    }

    private void validateSignature(String webhookId, String signature, String timestamp, String rawPayload) {
        String signedPayload = webhookId + "." + timestamp + "." + rawPayload;

        try {
            String actual = extractSignature(signature);

            String secretText = webhookSecret;
            if (secretText.startsWith("whsec_")) {
                secretText = secretText.substring("whsec_".length());
            }

            byte[] keyBytes = Base64.getDecoder().decode(secretText);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKeySpec);

            String expected = Base64.getEncoder()
                    .encodeToString(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

            log.info("expected = [{}]", expected);
            log.info("actual   = [{}]", actual);

            if (!expected.equals(actual)) {
                throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
            }
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ServiceException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }
    }

    private String extractSignature(String signatureHeader) {
        String[] parts = signatureHeader.split(",", 2);
        if (parts.length != 2 || !"v1".equals(parts[0].trim())) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
        return parts[1].trim();
    }
}

