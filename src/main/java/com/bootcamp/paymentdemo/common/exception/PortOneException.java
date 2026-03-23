package com.bootcamp.paymentdemo.common.exception;

public class PortOneException extends RuntimeException {

    private final ErrorCode errorCode;

    public PortOneException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
