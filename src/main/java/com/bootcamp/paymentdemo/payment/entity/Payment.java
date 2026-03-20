package com.bootcamp.paymentdemo.payment.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name="payments")
public class Payment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="payment_uid", nullable = false, unique = true)
    private String paymentUid;

    // TODO : imp_uid, pgTxId

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="order_id", nullable = false)
    private Order order;     // 주문 ID

    @Column(nullable = false)
    private Long amount;     // 결제 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;   // 결제 상태'

    private LocalDateTime paidAt;   // 결제 성공 시각

    // 결제 시도 시각과 상태 변경 시각은 BaseEntity 필드값으로 관리


    @Builder
    public Payment(String paymentUid, Order order, Long amount, PaymentStatus paymentStatus) {
        this.paymentUid = paymentUid;
        this.order = order;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
    }

    // 결제 완료로 전환하는 메서드
    public void paid() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new ServiceException(ErrorCode.PAYMENT_STATUS_NOT_PENDING);
        }
        this.paymentStatus = PaymentStatus.SUCCESS;
    }
    // 결제상태 환불완료로 전환메서드
    public void refund() {
        if (this.paymentStatus != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
    }
}
