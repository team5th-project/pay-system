package com.bootcamp.paymentdemo.security.token;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String token; // 리프레시 토큰 값

    @Builder
    public RefreshToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public void updateToken(String refreshToken){
        this.token = refreshToken;
    }
}