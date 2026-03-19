package com.bootcamp.paymentdemo.security;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 토큰 인증 필터
 * 모든 요청에서 JWT 토큰을 검증하고 SecurityContext에 인증 정보 설정
 *
 * TODO: 개선 사항
 * - 역할(Role) 정보를 토큰에서 추출
 * - 예외 처리 개선
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Request Header에서 JWT 토큰 추출
            String token = getJwtFromRequest(request);

            boolean isJwtValid = false;
            try{
                isJwtValid = jwtTokenProvider.validateToken(token);
            }catch (ServiceException e){
                request.setAttribute("exception", e.getErrorCode());
            }
            if (token != null && isJwtValid) {

                //인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        jwtTokenProvider.getAuthentication(token);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("JWT 인증 실패", e);
            throw e;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 JWT 토큰 추출
     * Authorization: Bearer {token}
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
