package com.bootcamp.paymentdemo.webhook.service;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.webhook.dto.PortOneWebhookRequest;
import com.bootcamp.paymentdemo.webhook.entity.WebhookEvent;
import com.bootcamp.paymentdemo.webhook.enums.PortOneEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bootcamp.paymentdemo.common.exception.ErrorCode.DUPLICATE_WEBHOOK;
import static com.bootcamp.paymentdemo.common.exception.ErrorCode.INVALID_WEBHOOK_EVENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventService2 { // TODO : 이름 변경

    private final PortOneWebhookVerifier portOneWebhookVerifier;
    private final WebhookEventRecorder webhookEventRecorder;
    private final PaymentWebhookProcessor paymentWebhookProcessor;

    @Transactional
    public void handleWebhook(String webhookId, String signature, String timestamp, String rawPayload) {
        WebhookEvent event = null;
        try {
            PortOneWebhookRequest request =
                    portOneWebhookVerifier.verifyAndParse(webhookId, signature, timestamp, rawPayload);

            String paymentUid = request.data().paymentId();
            String eventType = request.type();
            PortOneEventType portOneEventType = PortOneEventType.from(eventType);

            event = webhookEventRecorder.recordReceived(
                    webhookId,
                    portOneEventType,
                    paymentUid,
                    rawPayload
            );

            if (portOneEventType == PortOneEventType.UNKNOWN) {
                log.info("지원하지 않는 이벤트 - webhookId={}, type={}", webhookId, request.type());

                webhookEventRecorder.markIgnored(event, "지원하지 않는 이벤트");
                return;
            }
            paymentWebhookProcessor.process(request,portOneEventType);

            webhookEventRecorder.markProcessed(event);

        } catch (ServiceException e) {
            if (event != null) {
                webhookEventRecorder.markFailed(event, e);
            }

            if (e.getErrorCode() == DUPLICATE_WEBHOOK) {
                log.info("중복 웹훅 무시 - webhookId={}", webhookId);
                return;
            }

            if (e.getErrorCode() == INVALID_WEBHOOK_EVENT) {
                log.info("지원하지 않는 이벤트 - webhookId={}", webhookId);
                return;
            }

                log.error("웹훅 처리 실패 - webhookId={}", webhookId, e);
                return;

            } catch (Exception e) {
                if (event != null) {
                    webhookEventRecorder.markFailed(event, e);
                }
                log.error("웹훅 처리 실패 - webhookId={}", webhookId, e);
                return;
        }
    }
}