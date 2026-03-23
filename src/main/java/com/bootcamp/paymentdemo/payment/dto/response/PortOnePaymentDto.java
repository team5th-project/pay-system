package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;

public record PortOnePaymentDto(
        PortOnePaymentStatus status,
        String paymentId,
        String transactionId,
        Long amount
) {
    public static PortOnePaymentDto from(PortOneResponse response) {
        return new PortOnePaymentDto(
                response.status(),
                response.id(),
                response.transactionId(),
                response.amount().paid()
        );
    }
}