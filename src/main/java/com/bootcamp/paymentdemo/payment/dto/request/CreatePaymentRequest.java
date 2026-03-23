package com.bootcamp.paymentdemo.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreatePaymentRequest {

    @NotNull(message = "결제 금액은 필수 입력값입니다.")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
    private Long totalAmount;

    @Min(value = 1,message = "포인트는 1 이상이어야 합니다.") // null이 아닐 경우에는 1포인트 이상 사용하도록 하기
    private Integer pointToUse;  // Integer 타입이어야 null 허용
}
