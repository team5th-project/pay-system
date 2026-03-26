package com.bootcamp.paymentdemo.payment.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final PendingPaymentRecoveryService pendingPaymentRecoveryService;
    private final PaymentCancelRecoveryService paymentCancelRecoveryService;

    // 30초마다 만료된 pending 결제 정리
    @Scheduled(fixedDelay = 30000)
    public void recoverExpiredPendingPayments() {
        log.info("[PaymentScheduler] recoverExpiredPendingPayments start");
        pendingPaymentRecoveryService.recoverExpiredPendingPayments();
        log.info("[PaymentScheduler] recoverExpiredPendingPayments end");
    }

    // 1분마다 취소 후속 처리
    @Scheduled(fixedDelay = 60000)
    public void recoverCancelPayments() {
        log.info("[PaymentScheduler] recoverCancelPayments start");
        paymentCancelRecoveryService.recoverCancelPayments();
        log.info("[PaymentScheduler] recoverCancelPayments end");
    }
}