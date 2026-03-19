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
    JWT_NOT_FOUND(HttpStatus.NOT_FOUND, "U005", "토큰을 찾을 수 없습니다."),

    //상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "P001", "유효하지 않은 카테고리입니다."),

    //주문
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "O001", "잘못된 주문 상태 전이입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "주문을 찾을 수 없습니다."),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "O003", "본인의 주문이 아닙니다."),



    //결제
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAY001", "유효하지 않은 결제 금액입니다."),
    INVALID_ORDER_UID(HttpStatus.BAD_REQUEST,"PAY002","유효하지 않은 주문 아이디입니다."),
    ORDER_STATUS_NOT_PENDING(HttpStatus.BAD_REQUEST, "PAY003", "주문 상태가 결제 대기 상태가 아닙니다."),
    ALREADY_PENDING_PAYMENT(HttpStatus.BAD_REQUEST,"PAY004","이미 결제 생성되어 결제 대기중인 주문입니다."),

    //환불
    INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "R001", "환불은 요청 상태에서만 상태 변경이 가능합니다."),


    //포인트
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "PT001", "포인트 잔액이 부족합니다.")


    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}