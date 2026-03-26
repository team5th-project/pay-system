package com.bootcamp.paymentdemo.refund.service;

import java.time.LocalDateTime;

// 자동 재시도 규칙 정책
public final class RefundRetryPolicy {
    private static final int FIRST_RETRY_MINUTES = 1;
    private static final int SECOND_RETRY_MINUTES = 5;
    private static final int THIRD_RETRY_MINUTES = 15;
    private static final int DEFAULT_RETRY_MINUTES = 30;
    private static final int MAX_RETRY_COUNT = 5;

    private RefundRetryPolicy() {}

    public static LocalDateTime calculateNextRetryAt(int retryCount, LocalDateTime now) {
        return switch (retryCount) {
            case 0 -> now.plusMinutes(FIRST_RETRY_MINUTES);
            case 1 -> now.plusMinutes(SECOND_RETRY_MINUTES);
            case 2 -> now.plusMinutes(THIRD_RETRY_MINUTES);
            default -> now.plusMinutes(DEFAULT_RETRY_MINUTES);
        };
    }
    // 자동 재시도 한도 초과 여부 확인
    // -> 환불 자체가 불가능하다는 뜻이 아니라 자동 재시도 종료 기준
    public static boolean isAutoRetryExhausted(int retryCount) {
        return retryCount >= MAX_RETRY_COUNT;
    }
}
