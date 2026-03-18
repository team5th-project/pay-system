package com.bootcamp.paymentdemo.payment.dto.response;


import com.bootcamp.paymentdemo.payment.dto.request.CreatePaymentRequest;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;

public record CreatePaymentResponse(
        String paymentId,
        PaymentStatus paymentStatus
) {
    public static CreatePaymentResponse from(Payment payment){
        return new CreatePaymentResponse(
                payment.getPaymentUid(),payment.getPaymentStatus()
        );
    }

}
