package com.bootcamp.paymentdemo.refund.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.entity.Payment;
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

    // 결제금액=환불금액(전액)
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

        if (amount == null || amount <= 0) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 환불 요청 시 상태가 요청으로 변경
        return Refund.builder()
                .payment(payment)
                .reason(reason)
                .refundStatus(RefundStatus.REQUESTED)
                .build();
    }

    // 환불 완료시 상태전이
    public void complete() {
        // 환불 상태가 요청이 아닌 다른 상태일 시 예외처리
        if (this.refundStatus != RefundStatus.REQUESTED) {
            throw new ServiceException(ErrorCode.INVALID_REFUND_STATUS);
        }
        this.refundStatus = RefundStatus.COMPLETED;
        // 환불 완료하여 환불처리 시각 표시
        this.refundedAt = LocalDateTime.now();
    }

    // 환불 실패시 상태전이
    public void fail() {
        if (this.refundStatus != RefundStatus.REQUESTED) {
            throw new ServiceException(ErrorCode.INVALID_REFUND_STATUS);
        }
        this.refundStatus = RefundStatus.FAILED;
    }

    // 환불 실패시 재시도 상태변경 메서드
    public void retry(String reason) {
        if (this.refundStatus != RefundStatus.FAILED) {
            throw new ServiceException(ErrorCode.INVALID_REFUND_STATUS);
        }
        this.reason = reason;
        this.refundStatus = RefundStatus.REQUESTED; // 요청 상태로 다시 변환
        this.refundedAt = null;
    }
}
