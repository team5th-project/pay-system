package com.bootcamp.paymentdemo.webhook.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.webhook.entity.WebhookEvent;
import com.bootcamp.paymentdemo.webhook.enums.PortOneEventType;
import com.bootcamp.paymentdemo.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WebhookEventRecorder {

    private final WebhookEventRepository webhookEventRepository;

    @Transactional
    public WebhookEvent recordReceived(String webhookId, PortOneEventType eventType, String paymentUid,String rawPayload
    ) {
        WebhookEvent event = WebhookEvent.createReceive(webhookId, paymentUid,eventType,rawPayload);

        try {
            return webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(ErrorCode.DUPLICATE_WEBHOOK);
        }
    }

    @Transactional
    public void markProcessed(WebhookEvent event) {
        event.markProcessed();
    }

    @Transactional
    public void markFailed(WebhookEvent event, Exception e) {
        event.markFailed(e.getMessage());
    }

    @Transactional
    public void markIgnored(WebhookEvent event, String reason) {
        event.markIgnored(reason);
    }
}