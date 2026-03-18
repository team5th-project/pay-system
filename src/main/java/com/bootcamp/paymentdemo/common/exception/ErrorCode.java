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
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "U001", "만료된 토큰입니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "U002", "사용할 수 없는 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "사용자를 찾을 수 없습니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "U004", "잘못된 비밀번호입니다."),

    //상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "P001", "유효하지 않은 카테고리입니다."),

    //주문




    //결제
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAY001", "유효하지 않은 결제 금액입니다."),


    //환불
    INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "R001", "환불은 요청 상태에서만 상태 변경이 가능합니다.")


    //포인트


    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}