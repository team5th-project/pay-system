package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;

public record ConfirmPaymentResponse(
        /*
        {
            "orderId" : "ORDER-uuid",
			"status" : "COMPLETED"
         */
        String orderId,
        PaymentStatus status
) {
    public static ConfirmPaymentResponse of(String orderId, PortOnePaymentDto portOnePaymentDto) {
        return new ConfirmPaymentResponse(orderId, PaymentStatus.from(portOnePaymentDto.status()));
    }
}