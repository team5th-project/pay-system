package com.bootcamp.paymentdemo.common.exception;

import lombok.Getter;

@Getter
public class PortOneException extends RuntimeException {

    private final ErrorCode errorCode;

    public PortOneException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
