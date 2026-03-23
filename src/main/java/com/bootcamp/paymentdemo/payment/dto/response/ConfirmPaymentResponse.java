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
    public static ConfirmPaymentResponse of(String orderId, PaymentStatus status) {
        return new ConfirmPaymentResponse(orderId, status);
    }
}