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
    


    //주문
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "O001", "잘못된 주문 상태 전이입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "주문을 찾을 수 없습니다."),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "O003", "본인의 주문이 아닙니다."),



    //결제




    //포인트




    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}