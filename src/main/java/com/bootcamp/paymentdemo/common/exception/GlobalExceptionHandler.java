package com.bootcamp.paymentdemo.common.exception;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // custom error (공통 에러 응답)
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

    // valid 에러 (dto 에서 정해주는거)
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

    // DB constraint 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonResponse<Object>> handleValidException(
            DataIntegrityViolationException e,
            HttpServletRequest request
    ){
        String errorMessage = "DB 저장 과정에서 예상치 못한 오류가 발생했습니다.";
        ErrorResponse errorResponse = getErrorResponse(
                ErrorCode.DB_ERROR, errorMessage, request.getRequestURI(), request.getMethod());
        return CommonResponseHandler.error(errorResponse);
    }

    // json 에 값이 잘못 들어간 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        String errorMessage = "잘못된 요청 형식입니다. JSON 문법을 확인해 주세요.";
        ErrorResponse errorResponse = getErrorResponse(
                ErrorCode.REQUEST_ERROR, errorMessage, request.getRequestURI(), request.getMethod()
        );
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