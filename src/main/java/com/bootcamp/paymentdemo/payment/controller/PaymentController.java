package com.bootcamp.paymentdemo.payment.controller;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.payment.dto.request.CreatePaymentRequest;
import com.bootcamp.paymentdemo.payment.dto.response.ConfirmPaymentResponse;
import com.bootcamp.paymentdemo.payment.dto.response.CreatePaymentResponse;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public ResponseEntity<CommonResponse<CreatePaymentResponse>> createPayment(
            @PathVariable String orderId,
            @Valid @RequestBody CreatePaymentRequest request){

        CreatePaymentResponse response = paymentService.createPayment(orderId, request);

        return CommonResponseHandler.success(HttpStatus.CREATED,response);
    }

    @PostMapping("/{paymentId}/confirm")
    public ResponseEntity<CommonResponse<ConfirmPaymentResponse>> completePayment(
            @PathVariable String paymentId){

        ConfirmPaymentResponse response = paymentService.confirmPayment(paymentId);
        return CommonResponseHandler.success(HttpStatus.OK,response);
    }

}
