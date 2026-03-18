package com.bootcamp.paymentdemo.security;


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
        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .errorName("LOGIN_ERROR")
                .message("로그인이 필요합니다.")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .build();

        CommonResponse<Object> commonResponse = CommonResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .data(null)
                .error(errorResponse)
                .build();

        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        objectMapper.writeValue(response.getWriter(), commonResponse);
    }
}
