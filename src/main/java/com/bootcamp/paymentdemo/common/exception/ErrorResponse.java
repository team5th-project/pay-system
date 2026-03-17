package com.bootcamp.paymentdemo.common.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
    private final int status;
    private final String errorName;
    private final String errorCode;
    private final String message;
    private final String path;
    private final String method;
}

