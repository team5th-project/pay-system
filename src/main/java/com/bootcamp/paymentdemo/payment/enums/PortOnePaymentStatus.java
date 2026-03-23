package com.bootcamp.paymentdemo.payment.enums;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;

public enum PortOnePaymentStatus {
    PAID,
    FAILED,
    CANCELLED,
    READY,
    PAY_PENDING;


    public static PortOnePaymentStatus from(String status) {
        return switch (status) {
            case "READY" -> PortOnePaymentStatus.READY;
            case "PAID" -> PortOnePaymentStatus.PAID;
            case "FAILED" -> PortOnePaymentStatus.FAILED;
            case "CANCELLED" -> PortOnePaymentStatus.CANCELLED;
            case "PAY_PENDING" -> PortOnePaymentStatus.PAY_PENDING;
            default -> throw new ServiceException(ErrorCode.UNKNOWN_PORTONE_STATUS);
        };
    }

}
