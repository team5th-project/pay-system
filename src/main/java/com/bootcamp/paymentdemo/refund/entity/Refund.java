package com.bootcamp.paymentdemo.refund.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.refund.enums.RefundFailureCode;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Refund extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 현재 정책: 결제 1건당 전액 환불 1회만 관리
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", unique = true)
    private Payment payment;

    // 환불 사유
    @Column(nullable = false)
    private String reason;

    // 환불 상태 (요청/완료/실패)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus;

    // 환불 처리시각
    private LocalDateTime refundedAt;

    // 실패코드
    @Enumerated(EnumType.STRING)
    private RefundFailureCode failureCode;

    // 실패사유
    @Column(length = 500)
    private String failureReason;

    // 재시도 횟수
    @Column(nullable = false)
    private int retryCount = 0;

    // 실패시각
    private LocalDateTime failedAt;

    // 재시도 시각
    private LocalDateTime nextRetryAt;

    // 마지막으로 재시도를 시도한 시각
    private LocalDateTime lastRetryAt;

    @Builder
    private Refund(Payment payment,
                   String reason,
                   RefundStatus refundStatus) {
        this.payment = payment;
        this.reason = reason;
        this.refundStatus = refundStatus;
    }

    // 환불 요청 메서드
    public static Refund create(Payment payment, String reason) {
        Long amount = payment.getFinalAmount();
        int pointToUse = payment.getPointToUse();

        boolean hasPaymentAmount = amount != null && amount > 0;
        boolean hasPointAmount = pointToUse > 0;

        // 둘 다 없을 때만 예외
        if (!hasPaymentAmount && !hasPointAmount) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 새 환불 엔티티 생성 시 초기 상태는 REQUESTED
        return Refund.builder()
                .payment(payment)
                .reason(reason)
                .refundStatus(RefundStatus.REQUESTED)
                .build();
    }

    // 환불 진행중 상태변환
    public void markProcessing() {
        if (this.refundStatus == RefundStatus.COMPLETED) {
            return;
        }
        if (this.refundStatus == RefundStatus.FAILED_FINAL) {
            throw new ServiceException(ErrorCode.INVALID_REFUND_STATUS);
        }
        this.refundStatus = RefundStatus.PROCESSING;
    }
    // 환불 성공 상태변환
    public void markCompleted() {
        if (this.refundStatus == RefundStatus.COMPLETED) {
            return;
        }
        this.refundStatus = RefundStatus.COMPLETED;
        this.refundedAt = LocalDateTime.now();
        this.failureCode = null;
        this.failureReason = null;
        this.failedAt = null;
        this.nextRetryAt = null;
    }

    // 환불 최종실패(관리자 수동체크 필요)
    public void markFinalFailure(RefundFailureCode failureCode, String failureReason) { //TODO 넷째, markFinalFailure() 는 retryCount 를 안 올리고 있다.
        this.refundStatus = RefundStatus.FAILED_FINAL;
        this.failureCode = failureCode;
        this.failureReason = trimReason(failureReason);
        this.failedAt = LocalDateTime.now();
        this.lastRetryAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    // 재시도 가능한 실패 상태로 전환
    public void markRetryableFailure(RefundFailureCode failureCode, String failureReason, LocalDateTime nextRetryAt) {
        this.refundStatus = RefundStatus.FAILED_RETRYABLE;
        this.failureCode = failureCode;
        this.failureReason = trimReason(failureReason);
        this.failedAt = LocalDateTime.now();
        this.lastRetryAt = LocalDateTime.now();
        this.nextRetryAt = nextRetryAt;
        this.retryCount += 1;
    }

    // 재시도 가능여부 체크
    public boolean isRetryDue(LocalDateTime now) {
        return this.refundStatus == RefundStatus.FAILED_RETRYABLE
                && this.nextRetryAt != null
                && !this.nextRetryAt.isAfter(now);
    }

    // // 외부 시스템에서 전달된 실패 메시지가 컬럼 길이를 초과하지 않도록 제한
    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }
}