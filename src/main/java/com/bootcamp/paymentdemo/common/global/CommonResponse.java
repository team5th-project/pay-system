package com.bootcamp.paymentdemo.common.global;

import com.bootcamp.paymentdemo.common.exception.ErrorResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class CommonResponse<T> {
    private final LocalDateTime timestamp;
    private final boolean success;
    private final int status;
    private final T data;
    private final ErrorResponse error;
}