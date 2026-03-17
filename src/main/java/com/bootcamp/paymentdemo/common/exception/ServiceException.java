package com.bootcamp.paymentdemo.common.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    //Runtime 중에 발생하는 오류를 잡는
    // 전역 Exception
    private final ErrorCode errorCode;

    //사용할때
    // ServiceException(ErrorCode.{ENUM 에 저장된 Error Code}) 로 씀
    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
