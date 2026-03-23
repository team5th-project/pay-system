package com.bootcamp.paymentdemo.payment.enums;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCEL_REQUESTED, // 실제 결제가 되었는데 재고 문제 등으로 결제 취소를 해야 하는 경우 등 내부 후처리 실패시
    CANCELLED;


    public static PaymentStatus from(PortOnePaymentStatus status) {
        return switch (status) {
            case READY,PAY_PENDING -> PaymentStatus.PENDING;
            case PAID -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
            case CANCELLED -> PaymentStatus.REFUNDED;
        };
    }
}
