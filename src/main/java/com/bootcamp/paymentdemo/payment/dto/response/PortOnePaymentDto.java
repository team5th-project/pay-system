package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;


public record PortOnePaymentDto(
        String id,  // transactionId
        String paymentUid,
        Long amount,
        PortOnePaymentStatus status
) {
    public static PortOnePaymentDto from(PortOneResponse portOneResponse) {
        PortOneResponse.PaymentTransaction paymentTransaction
                = portOneResponse.items().stream().findFirst().get();

        PortOneResponse.PaymentTransaction.PaymentAmount paymentAmount = paymentTransaction.amount().stream().findFirst().get();

        String id = paymentTransaction.id();
        String paymentUid = paymentTransaction.paymentId();
        Long amount = (long) paymentAmount.paid();
        PortOnePaymentStatus status = PortOnePaymentStatus.from(paymentTransaction.status().toString());

        return new PortOnePaymentDto(id, paymentUid,amount,status);
    }
}
