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
    DB_ERROR(HttpStatus.BAD_REQUEST, "T003", "DB 저장 에러"),
    REQUEST_ERROR(HttpStatus.BAD_REQUEST, "T004", "json 요청 형식 오류"),
    //사용자
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "U001", "만료된 토큰입니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "U002", "사용할 수 없는 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U003", "사용자를 찾을 수 없습니다."),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "U004", "잘못된 비밀번호입니다."),
    JWT_NOT_FOUND(HttpStatus.NOT_FOUND, "U005", "토큰을 찾을 수 없습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "U006", "로그인이 필요합니다."),

    //상품
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "상품을 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "P002", "유효하지 않은 카테고리입니다."),

    //주문 상품
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "주문 상품을 찾을 수 없습니다."),
    STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "P004", "재고가 부족합니다."),

    INVALID_PRODUCT_QUANTITY(HttpStatus.BAD_REQUEST, "P005", "상품 수량은 1개 이상이어야 합니다."),
    //주문
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "O001", "잘못된 주문 상태 전이입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O002", "주문을 찾을 수 없습니다."),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "O003", "본인의 주문이 아닙니다."),



    // 결제
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "PAY001", "유효하지 않은 결제 금액입니다."),
    INVALID_ORDER_UID(HttpStatus.BAD_REQUEST, "PAY002", "유효하지 않은 주문 아이디입니다."),
    ORDER_STATUS_NOT_PENDING(HttpStatus.BAD_REQUEST, "PAY003", "주문 상태가 결제 대기 상태가 아닙니다."),
    ALREADY_PENDING_PAYMENT(HttpStatus.BAD_REQUEST, "PAY004", "이미 결제 생성되어 결제 대기중인 주문입니다."),
    PAYMENT_UID_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY005", "유효하지 않은 결제 아이디입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY006", "결제 정보를 찾을 수 없습니다."),
    ALREADY_PROCESSED_PAYMENT(HttpStatus.BAD_REQUEST, "PAY007", "이미 처리된 결제 요청입니다."),
    INVALID_PAYMENT_UID(HttpStatus.BAD_REQUEST, "PAY008", "유효하지 않은 결제 아이디입니다."),
    NOT_PAID_YET(HttpStatus.BAD_REQUEST, "PAY009", "아직 결제되지 않은 결제 요청입니다."),
    PAYMENT_AMOUNT_NOT_EQUALS(HttpStatus.BAD_REQUEST, "PAY010", "결제 금액이 일치하지 않습니다."),
    PAYMENT_STATUS_NOT_PENDING(HttpStatus.BAD_REQUEST, "PAY011", "결제 상태가 결제 대기 상태가 아닙니다."),
    UNKNOWN_PORTONE_STATUS(HttpStatus.NOT_FOUND, "PAY012", "존재하지 않는 포트원 상태입니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAY013", "결제에 실패하였습니다."),
    PAYMENT_CHECK_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PAY014", "포트원 결제 조회에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "PAY015", "결제 취소에 실패하였습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "ROO2", "결제 상태가 유효하지 않습니다."),

    // 포트원 에러
    INVALID_PAYMENT_VALIDATION_REQUEST(HttpStatus.BAD_REQUEST, "PAY005", "잘못된 결제 검증 요청입니다."),
    UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST(HttpStatus.UNAUTHORIZED, "PAY006", "포트원 인증에 실패했습니다."),
    PORTONE_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAY007", "결제 내역을 찾을 수 없습니다."),
    PORTONE_PAYMENT_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "PAY008", "결제 검증에 실패했습니다."),
    PORTONE_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "PAY009", "포트원 서버 오류가 발생했습니다."),
    PORTONE_UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PAY010", "알 수 없는 오류가 발생했습니다."),
    PORTONE_COMMUNICATION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "PAY011", "네트워크 통신에 실패했습니다."),
    PORTONE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PAY012", "포트원 API를 호출에 실패했습니다."),

    //환불
    INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "R001", "현재 환불 상태에서는 요청한 작업을 수행할 수 없습니다."),
    INVALID_PAYMENT_STATUS_FOR_REFUND(HttpStatus.BAD_REQUEST, "ROO2", "환불은 결제 완료 상태에서만 가능합니다."),
    REFUND_FAILED(HttpStatus.BAD_REQUEST,"R003", "환불 처리 중 오류가 발생했습니다."),
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "환불내역을 찾을 수 없습니다."),
    REFUND_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "R005", "환불 가능 기간이 지났습니다."),
    INVALID_REFUND_AMOUNT(HttpStatus.BAD_REQUEST, "R006", "환불금액이 올바르지 않습니다."),

    //웹훅
    INVALID_WEBHOOK_STATUS(HttpStatus.BAD_REQUEST, "W001", "처리할 수 없는 웹훅 상태입니다."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "W002", "웹훅 서명이 유효하지 않습니다."),
    INVALID_WEBHOOK_TIMESTAMP(HttpStatus.BAD_REQUEST, "W003", "웹훅 요청시간이 유효하지 않습니다."),
    INVALID_WEBHOOK_PAYLOAD(HttpStatus.BAD_REQUEST, "W004", "웹훅 데이터 형식이 올바르지 않습니다."),
    INVALID_WEBHOOK_WEBHOOKID(HttpStatus.BAD_REQUEST, "W005", "웹훅 아이디가 존재하지 않거나 유효하지 않습니다."),
    INVALID_WEBHOOK_EVENT(HttpStatus.BAD_REQUEST, "W006", "지원하지 않는 웹훅 이벤트입니다."),
    DUPLICATE_WEBHOOK(HttpStatus.BAD_REQUEST,"W007","중복된 웹훅입니다."),
    INVALID_WEBHOOK_HEADER(HttpStatus.BAD_REQUEST, "W008", "웹훅 헤더가 유효하지 않습니다."),
    WEBHOOK_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "W009", "웹훅 검증에 실패했습니다"),

    //포인트
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "PT001", "포인트 잔액이 부족합니다."),
    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "PT002", "포인트는 양수여야 합니다."),
    USERPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "PT003", "유저 포인트 정보를 찾을 수 없습니다."),

    // 멤버십
    MEMBERSHIP_POLICY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "MS001", "멤버십 등급 정책을 찾을 수 없습니다."),
    INVALID_MEMBERSHIP_GRADE(HttpStatus.BAD_REQUEST, "MS002", "유효하지 않은 멤버십 등급입니다."),
    MEMBERSHIP_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MS003", "멤버십 등급 갱신에 실패했습니다.")




    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
