package com.bootcamp.paymentdemo.payment.enums;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;

public enum PortOnePaymentStatus {
    READY,
    PAID,
    FAILED,
    CANCELLED;


    public static PortOnePaymentStatus from(String status) {
        return switch (status) {
            case "READY" -> PortOnePaymentStatus.READY;
            case "PAID" -> PortOnePaymentStatus.PAID;
            case "FAILED" -> PortOnePaymentStatus.FAILED;
            case "CANCELLED" -> PortOnePaymentStatus.CANCELLED;
            default -> throw new ServiceException(ErrorCode.UNKNOWN_PORTONE_STATUS);
        };
    }

}
