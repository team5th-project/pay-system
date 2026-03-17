package com.bootcamp.paymentdemo.common.exception;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import jakarta.servlet.http.HttpServletRequest;

import com.bootcamp.paymentdemo.common.exception.ErrorResponse;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<CommonResponse<Object>> handleCustomException(
            ServiceException e,
            HttpServletRequest request
    ){
        ErrorResponse errorResponse = getErrorResponse(
                e.getErrorCode(), e.getErrorCode().getMessage(), request.getRequestURI(), request.getMethod()
        );
        return CommonResponseHandler.error(errorResponse);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Object>> handleValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ){
        String errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        ErrorResponse errorResponse = getErrorResponse(
                ErrorCode.VALID_ERROR, errorMessage, request.getRequestURI(), request.getMethod());
        return CommonResponseHandler.error(errorResponse);
    }

    private ErrorResponse getErrorResponse(ErrorCode errorCode, String message, String path, String method){
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .errorName(errorCode.toString())
                .message(message)
                .path(path)
                .method(method)
                .build();
    }
}