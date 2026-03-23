package com.bootcamp.paymentdemo.point.scheduler;

import com.bootcamp.paymentdemo.point.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointExpirationScheduler {
    private final UserPointService userPointService;

    // 포인트 만료 소멸 스케줄러
    // 자정 자동 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void expirePoint() {
        log.info("[PointExpirationScheduler] 포인트 소멸 스케줄러 실행 시작");
        userPointService.expirePoint();
        log.info("[PointExpirationScheduler] 포인트 소멸 스케줄러 실행 완료");
    }
}
