package com.bootcamp.paymentdemo.payment.enums;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED;

    public static PaymentStatus from(PortOnePaymentStatus status) {
        return switch (status) {
            case READY -> PaymentStatus.PENDING;
            case PAID -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
            case CANCELLED -> PaymentStatus.REFUNDED;
        };
    }
}
