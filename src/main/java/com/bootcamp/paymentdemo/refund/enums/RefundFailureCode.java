package com.bootcamp.paymentdemo.refund.enums;

// 실패 원인 기록 및 재시도 가능 여부 체크
public enum RefundFailureCode {
    PORTONE_SERVER_ERROR,
    PORTONE_COMMUNICATION_ERROR,
    PORTONE_UNKNOWN_ERROR,
    INVALID_PAYMENT_STATUS,
    INVALID_REFUND_STATUS,
    PORTONE_PAYMENT_NOT_FOUND,
    UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST;

    // 해당 실패가 재시도 가능한 유형인지 판단
    public boolean isRetryable() {
        return this == PORTONE_SERVER_ERROR
                || this == PORTONE_COMMUNICATION_ERROR
                || this == PORTONE_UNKNOWN_ERROR;
    }
}