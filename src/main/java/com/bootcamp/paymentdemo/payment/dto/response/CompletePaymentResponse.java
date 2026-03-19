package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;

public record CompletePaymentResponse(
        /*
        {
            "orderId" : "ORDER-uuid",
			"status" : "COMPLETED"
         */
        String orderId,
        PaymentStatus status
) {
}