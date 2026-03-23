package com.bootcamp.paymentdemo.payment.dto.response;


import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneResponse(
        PortOnePaymentStatus status,
        String id,
        String transactionId,
        PaymentAmount amount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentAmount(
            Long paid
    ) {}
}
