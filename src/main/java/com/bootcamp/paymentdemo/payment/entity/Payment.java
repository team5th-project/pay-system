package com.bootcamp.paymentdemo.payment.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
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

    @Column()
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
    public Payment(String paymentUid, String transactionId, Order order, Long amount, PaymentStatus paymentStatus) {
        this.paymentUid = paymentUid;
        this.transactionId = transactionId;
        this.order = order;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
    }
}
