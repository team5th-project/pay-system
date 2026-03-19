package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;

import java.util.List;

public record PortOneResponse(
        List<PaymentTransaction> items
){
    public record PaymentTransaction(
        PortOnePaymentStatus status,
        String id,  // transactionId
        String paymentId,
        List<PaymentAmount> amount
    ){
        public record PaymentAmount(
            int paid
        ){}

    }
}
