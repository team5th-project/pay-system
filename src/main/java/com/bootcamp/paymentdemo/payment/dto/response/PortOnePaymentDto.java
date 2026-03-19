package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;


public record PortOnePaymentDto(
        String id,  // transactionId
        String paymentUid,
        int paid,
        PortOnePaymentStatus status
) {
    public static PortOnePaymentDto from(PortOneResponse portOneResponse) {
        PortOneResponse.PaymentTransaction paymentTransaction
                = portOneResponse.items().stream().findFirst().get();
        PortOneResponse.PaymentTransaction.PaymentAmount paymentAmount = paymentTransaction.amount().stream().findFirst().get();

        return new PortOnePaymentDto(paymentTransaction.id(),
                paymentTransaction.paymentId(),
                paymentAmount.paid(),
                paymentTransaction.status());


    }
}
