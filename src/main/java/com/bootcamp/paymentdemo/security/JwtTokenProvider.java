package com.bootcamp.paymentdemo.security;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.security.token.BlacklistRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 * 개선할 부분: Refresh Token, Token Expiry 관리, Claims 커스터마이징 등
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret:commercehub-secret-key-for-demo-please-change-this-in-production-environment}")
    private String secret;

    @Value("${jwt.token-validity-in-seconds:86400}")
    private long tokenValidityInSeconds;

    private SecretKey secretKey;
    private long tokenValidityInMilliseconds;
    private final BlacklistRepository blacklistRepository;

    @PostConstruct // 의존성 주입이 완료된 후 실행됨
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
    }

    /**
     * JWT 토큰 생성
     *
     * TODO: 개선 사항
     * - 사용자 역할(Role) 정보 추가
     * - 추가 Claims 정보 (이름, 이메일 등)
     * - Refresh Token 발급 로직
     */
    public String createToken(Long id, String email, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
            .subject(String.valueOf(id))
            .claim("email", email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey)
            .compact();
    }

    public String createRefreshToken(Long id) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds * 7);

        return Jwts.builder()
                .subject(String.valueOf(id))
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();

    }

    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        Long userId = Long.parseLong(getId(token));
        String email = getEmail(token);
        String role = getRole(token);

        return new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(userId, email, role),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_"+role))
        );
    }

    public String getId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return claims.getSubject();
    }

    public String getRole(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * JWT 토큰에서 사용자 이름 추출
     */
    public String getEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * JWT 토큰 유효성 검증
     *
     * TODO: 개선 사항
     * - 토큰 블랙리스트 체크 (로그아웃된 토큰)
     * - 토큰 갱신 로직
     * - 상세한 예외 처리
     */
    public boolean validateToken(String token) {
        if (blacklistRepository.existsByToken(token)){
            //token 이 블랙리스트에 들어가있을때
            throw new ServiceException(ErrorCode.JWT_INVALID);
        }

        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;

            // TODO: 구체적인 예외 처리 구현
            // - ExpiredJwtException: 만료된 토큰
            // - MalformedJwtException: 잘못된 형식
            // - SignatureException: 서명 오류
        } catch (ExpiredJwtException e) {
            throw new ServiceException(ErrorCode.JWT_EXPIRED);
        } catch (MalformedJwtException | IllegalArgumentException e) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
            // 토큰이 애초에 형식이 이상하거나 비어있을때
        } catch (UnsupportedJwtException e) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
        } catch (SignatureException e) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
            // 토큰이 위조의 위험이 있습니다요 조심해!!!
        }
    }

    public LocalDateTime getExpirationDateTime(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        return expiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
