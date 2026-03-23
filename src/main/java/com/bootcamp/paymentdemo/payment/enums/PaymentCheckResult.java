package com.bootcamp.paymentdemo.payment.enums;

import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;

public record PaymentCheckResult(
        PaymentResult paymentResult,
        PortOnePaymentDto portOnePaymentDto
) {
}
