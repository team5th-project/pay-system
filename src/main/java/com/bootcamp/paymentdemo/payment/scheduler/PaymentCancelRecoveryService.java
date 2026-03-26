package com.bootcamp.paymentdemo.payment.scheduler;

import com.bootcamp.paymentdemo.common.exception.PortOneException;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentCancelResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.payment.service.PaymentCancelService;
import com.bootcamp.paymentdemo.payment.service.PaymentStatusTxService;
import com.bootcamp.paymentdemo.payment.service.PortOneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelRecoveryService {
    /*
    CANCEL_REQUESTED
    CANCEL_FAILED
    modifiedAt < 지금 - 30초
    최대 30건
     */

    private static final int CANCEL_BATCH_SIZE = 30;
    private static final long CANCEL_RETRY_DELAY_SECONDS = 30L;
    private static final String CANCEL_REASON = "Scheduler retry cancel payment";

    private final PaymentRepository paymentRepository;
    private final PortOneService portOneService;
    private final PaymentCancelService paymentCancelService;
    private final PaymentStatusTxService paymentStatusTxService;

    @Transactional(readOnly = true)
    public void recoverCancelPayments() {
        List<Payment> targets =
                paymentRepository.findByPaymentStatusInAndModifiedAtBeforeOrderByIdAsc(
                        List.of(PaymentStatus.CANCEL_REQUESTED, PaymentStatus.CANCEL_FAILED),
                        LocalDateTime.now().minusSeconds(CANCEL_RETRY_DELAY_SECONDS),
                        PageRequest.of(0, CANCEL_BATCH_SIZE)
                );

        if (targets.isEmpty()) {
            log.debug("[CancelRecovery] no cancel recovery targets");
            return;
        }

        for (Payment payment : targets) {
            try {
                recoverOne(payment.getPaymentUid());
            } catch (Exception e) {
                log.error("[CancelRecovery] recover failed. paymentUid={}", payment.getPaymentUid(), e);
            }
        }
    }

    public void recoverOne(String paymentUid) {
        Payment payment = paymentRepository.findByPaymentUid(paymentUid)
                .orElseThrow(() -> new IllegalArgumentException("payment not found: " + paymentUid));

        if (!isCancelRecoverTarget(payment)) {
            log.info("[CancelRecovery] skip - invalid status. paymentUid={}, status={}",
                    paymentUid, payment.getPaymentStatus());
            return;
        }

        PortOnePaymentDto portOnePaymentDto;
        try {
            portOnePaymentDto = portOneService.getPayment(paymentUid);
        } catch (PortOneException | RestClientException | ServiceException e) {
            log.warn("[CancelRecovery] getPayment failed. paymentUid={}", paymentUid, e);
            return;
        }

        PortOnePaymentStatus portOneStatus = portOnePaymentDto.status();

        switch (portOneStatus) {
            case CANCELLED -> completeCancel(paymentUid);
            case PAID -> retryCancel(payment);
            case FAILED, READY, PAY_PENDING -> {
                log.info("[CancelRecovery] keep current state. paymentUid={}, portOneStatus={}",
                        paymentUid, portOneStatus);
            }
        }
    }

    private boolean isCancelRecoverTarget(Payment payment) {
        return payment.getPaymentStatus() == PaymentStatus.CANCEL_REQUESTED
                || payment.getPaymentStatus() == PaymentStatus.CANCEL_FAILED;
    }

    private void completeCancel(String paymentUid) {
        try {
            paymentStatusTxService.markCancelled(paymentUid);
            log.info("[CancelRecovery] cancel completed. paymentUid={}", paymentUid);
        } catch (ServiceException e) {
            log.warn("[CancelRecovery] markCancelled skipped. paymentUid={}", paymentUid, e);
        }
    }

    private void retryCancel(Payment payment) {
        String paymentUid = payment.getPaymentUid();

        PaymentCancelResult cancelResult =
                paymentCancelService.processPaymentCancel(payment, CANCEL_REASON);

        switch (cancelResult) {
            case SUCCESS -> {
                try {
                    paymentStatusTxService.markCancelled(paymentUid);
                    log.info("[CancelRecovery] retry cancel success. paymentUid={}", paymentUid);
                } catch (ServiceException e) {
                    log.error("[CancelRecovery] markCancelled failed after retry. paymentUid={}", paymentUid, e);
                }
            }

            case CANCEL_REQUESTED -> {
                log.info("[CancelRecovery] cancel requested remains. paymentUid={}", paymentUid);
            }

            case FAIL -> {
                // CANCEL_FAILED 상태인 건 다시 markCancelFailed() 하면 엔티티 검증에서 막힐 수 있음
                if (payment.getPaymentStatus() == PaymentStatus.CANCEL_REQUESTED) {
                    try {
                        paymentStatusTxService.markCancelFailed(paymentUid);
                        log.warn("[CancelRecovery] retry cancel failed. paymentUid={}", paymentUid);
                    } catch (ServiceException e) {
                        log.warn("[CancelRecovery] markCancelFailed skipped. paymentUid={}", paymentUid, e);
                    }
                } else {
                    log.warn("[CancelRecovery] retry cancel failed but already CANCEL_FAILED. paymentUid={}", paymentUid);
                }
            }
        }
    }
}