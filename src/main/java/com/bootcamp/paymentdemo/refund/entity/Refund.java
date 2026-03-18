package com.bootcamp.paymentdemo.refund.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Refund extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus refundStatus;

    @Column(nullable = false)
    private LocalDateTime refundedAt;

    @Builder
    public Refund(Payment payment,
                  String reason,
                  RefundStatus refundStatus,
                  LocalDateTime refundedAt) {
        this.payment = payment;
        this.reason = reason;
        this.refundStatus = refundStatus;
        this.refundedAt = refundedAt;
    }

    public static Refund create(Payment payment, String reason) {
        Long amount = payment.getAmount();

        if (amount == null || amount <= 0) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        return Refund.builder()
                .payment(payment)
                .reason(reason)
                .refundStatus(RefundStatus.REQUESTED)
                .refundedAt(LocalDateTime.now())
                .build();
    }
}
