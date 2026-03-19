package com.bootcamp.paymentdemo.common.global;

import com.bootcamp.paymentdemo.common.exception.ErrorResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class CommonResponseHandler {

    // response DTO, 성공, header 도 있을 때
    public static <T> ResponseEntity<CommonResponse<T>> success(
            HttpStatus status,
            T data,
            HttpHeaders header
    ){
        CommonResponse<T> response = CommonResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .success(true)
                .status(status.value())
                .data(data)//개별 DTO
                .error(null)
                .build();

        return ResponseEntity.status(status).headers(header).body(response);
    }

    // response DTO 가 있고, 성공
    public static <T> ResponseEntity<CommonResponse<T>> success(
            HttpStatus status,
            T data
    ){
        return success(status, data, null);
    }

    // DTO 가 없는 경우, 성공 (오버로딩)
    public static ResponseEntity<CommonResponse<Void>> success(
            HttpStatus status
    ) {
        return success(status, null);
    }

    // error의 경우
    public static <T> ResponseEntity<CommonResponse<T>> error(
            ErrorResponse errorResponse
    ){
        CommonResponse<T> response = CommonResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .success(false)
                .status(errorResponse.getStatus())
                .data(null)
                .error(errorResponse)
                .build();

        return ResponseEntity.status(errorResponse.getStatus()).body(response);
    }
}
