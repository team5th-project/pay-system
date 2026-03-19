package com.bootcamp.paymentdemo.payment.dto.response;

import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;

import java.util.List;

public record PortOneResponse(
        List<PaymentTransaction> items
){
    public record PaymentTransaction(
            /*
            READY       // 결제 준비
            PAID        // 결제 성공
            FAILED      // 결제 실패
            CANCELED    // 결제 취소
             */
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
