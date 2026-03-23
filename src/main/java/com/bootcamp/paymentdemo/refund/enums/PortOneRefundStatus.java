package com.bootcamp.paymentdemo.refund.enums;

public enum PortOneRefundStatus {
    FAILED,
    REQUESTED,
    SUCCEEDED,
    UNKNOWN // 외부 시스템 상태 변경에 대한 방어
    ;

    public static PortOneRefundStatus from(String status) {
        if (status == null) return UNKNOWN;

        return switch (status.toUpperCase()) {
            case "SUCCEEDED" -> SUCCEEDED;
            case "REQUESTED" -> REQUESTED;
            case "FAILED" -> FAILED;
            default -> UNKNOWN;
        };
    }
}
