package com.bootcamp.paymentdemo.refund.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateRefundRequest {
    @NotBlank(message = "환불 사유는 필수값입니다.")
    @Size(max = 200, message = "환불 사유는 200자 내외로 입력해주세요.")
    private String reason;
}
