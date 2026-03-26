package com.bootcamp.paymentdemo.webhook.enums;

public enum WebhookStatus {
    RECEIVED, // 받음
    PROCESSED, // 처리 완료
    FAILED, // 처리 실패
    IGNORED;
}
