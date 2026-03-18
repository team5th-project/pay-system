package com.bootcamp.paymentdemo.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    //temp error code
    TEMP_ERROR(HttpStatus.BAD_REQUEST, "T001", "커스텀 에러가 발생했습니다."),
    VALID_ERROR(HttpStatus.BAD_REQUEST, "T002", "validError"),
    //사용자



    //상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "P001", "유효하지 않은 카테고리입니다."),

    //주문




    //결제
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAY001", "유효하지 않은 결제 금액입니다.")


    //환불



    //포인트




    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}