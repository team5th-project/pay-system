package com.bootcamp.paymentdemo.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BlacklistCleaner {

    private final BlacklistRepository blacklistRepository;

    // 1시간마다 만료된 토큰 삭제 (DB 기준 예시)
    @Scheduled(fixedDelay = 3600000)
    public void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        blacklistRepository.deleteByExpirationTimeBefore(now);
    }
}