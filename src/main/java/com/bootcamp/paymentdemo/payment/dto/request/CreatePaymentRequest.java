package com.bootcamp.paymentdemo.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreatePaymentRequest {

    @NotNull(message = "결제 금액은 필수 입력값입니다.")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
    private Long totalAmount;
}
