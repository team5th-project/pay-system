package com.bootcamp.paymentdemo.payment.scheduler;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentCancelResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.payment.service.PaymentCancelService;
import com.bootcamp.paymentdemo.payment.service.PaymentStatusTxService;
import com.bootcamp.paymentdemo.payment.service.PaymentVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentRecoveryService {

    private static final int PENDING_BATCH_SIZE = 50;
    private static final String CANCEL_REASON = "Expired pending payment recovery";

    private final PaymentRepository paymentRepository;
    private final PaymentVerificationService paymentVerificationService;
    private final PaymentCancelService paymentCancelService;
    private final PaymentStatusTxService paymentStatusTxService;

    public void recoverExpiredPendingPayments() {

        List<Payment> targets = paymentRepository.findByPaymentStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, PENDING_BATCH_SIZE)
        );

        if (targets.isEmpty()) {
            log.debug("[PendingRecovery] no expired pending payments");
            return;
        }

        for (Payment payment : targets) {
            try {
                recoverOne(payment);
            } catch (Exception e) {
                log.error("[PendingRecovery] failed. paymentUid={}", payment.getPaymentUid(), e);
            }
        }
    }

    public void recoverOne(Payment payment) {
        String paymentUid = payment.getPaymentUid();

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            log.info("[PendingRecovery] skip - not pending. paymentUid={}, status={}",
                    paymentUid, payment.getPaymentStatus());
            return;
        }

        if (payment.getExpiresAt() == null || payment.getExpiresAt().isAfter(LocalDateTime.now())) {
            log.info("[PendingRecovery] skip - not expired yet. paymentUid={}", paymentUid);
            return;
        }

        // 정책:
        // 만료된 PENDING 결제는
        // 1) 실제 결제가 성공했으면 취소 요청
        // 2) 실제 결제가 안됐거나 실패했으면 FAILED 처리

        PaymentResult result = paymentVerificationService.checkPayment(payment);

        switch (result) {
            case SUCCESS, AMOUNT_MISMATCH -> requestCancelAfterInternalFailure(payment);
            case FAIL, CANCELLED -> handleFail(paymentUid);
        }
    }

    private void handleFail(String paymentUid) {
        try {
            paymentStatusTxService.markFailed(paymentUid);
            log.info("[PendingRecovery] payment failed. paymentUid={}", paymentUid);
        } catch (ServiceException e) {
            log.warn("[PendingRecovery] markFailed skipped. paymentUid={}", paymentUid, e);
        }
    }

    private void requestCancelAfterInternalFailure(Payment payment) {
        String paymentUid = payment.getPaymentUid();

        try {
            paymentStatusTxService.markCancelRequested(paymentUid);
        } catch (ServiceException e) {
            log.warn("[PendingRecovery] markCancelRequested skipped. paymentUid={}", paymentUid, e);
            return;
        }

        PaymentCancelResult cancelResult = paymentCancelService.processPaymentCancel(payment, CANCEL_REASON);

        switch (cancelResult) {
            case SUCCESS -> {
                try {
                    paymentStatusTxService.markCancelled(paymentUid);
                    log.info("[PendingRecovery] cancel success. paymentUid={}", paymentUid);
                } catch (ServiceException e) {
                    log.error("[PendingRecovery] markCancelled failed. paymentUid={}", paymentUid, e);
                }
            }
            case CANCEL_REQUESTED ->
                    log.info("[PendingRecovery] cancel requested remains. paymentUid={}", paymentUid);

            case FAIL -> {
                try {
                    paymentStatusTxService.markCancelFailed(paymentUid);
                    log.warn("[PendingRecovery] cancel failed. paymentUid={}", paymentUid);
                } catch (ServiceException e) {
                    log.error("[PendingRecovery] markCancelFailed failed. paymentUid={}", paymentUid, e);
                }
            }
        }
    }
}