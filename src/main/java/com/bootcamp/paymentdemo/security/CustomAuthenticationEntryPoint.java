package com.bootcamp.paymentdemo.security;


import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ErrorResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorCode errorCode = (ErrorCode) request.getAttribute("exception");

        if (errorCode == null) {
            errorCode = ErrorCode.LOGIN_REQUIRED;
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(errorCode.getStatus().value()) // ErrorCode에 HttpStatus가 있다고 가정
                .errorName(errorCode.name())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        CommonResponse<Object> commonResponse = CommonResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .data(null)
                .error(errorResponse)
                .build();

        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        objectMapper.writeValue(response.getWriter(), commonResponse);
    }
}
