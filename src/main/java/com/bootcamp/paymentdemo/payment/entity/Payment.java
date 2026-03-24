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
    private Long finalAmount;     // 최종 결제 금액

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;   // 결제 상태'

    @Column(nullable = false)
    private int pointToUse;

    private LocalDateTime paidAt;   // 결제 성공 시각

    private LocalDateTime expiresAt;

    // 결제 시도 시각과 상태 변경 시각은 BaseEntity 필드값으로 관리


    @Builder
    public Payment(String paymentUid, Order order, Long finalAmount, int pointToUse, PaymentStatus paymentStatus, LocalDateTime expiresAt) {

        this.paymentUid = paymentUid;
        this.order = order;
        this.finalAmount = finalAmount;
        this.pointToUse = pointToUse;
        this.paymentStatus = paymentStatus;
        this.expiresAt = expiresAt;
    }

    // 결제 완료로 전환하는 메서드
    public void success() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new ServiceException(ErrorCode.PAYMENT_STATUS_NOT_PENDING);
        }
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }
    // 결제상태 환불완료로 전환메서드
    public void refund() {
        if (this.paymentStatus != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    // 결제상태 결제 실패로 전환하는 메서드
    public void failed() {
        if(this.paymentStatus!=PaymentStatus.PENDING){
            throw new ServiceException(ErrorCode.PAYMENT_STATUS_NOT_PENDING);
        }
        this.paymentStatus = PaymentStatus.FAILED;
    }

    // 결제 성공했다가 내부 사정으로 pg사로 다시 결제 취소 요청을 보내고 응답을 기다리는 상태!
    public void cancelRequested(){
        // 포트원 결제 성공 후 재고 차감 문제로 취소 요청이 보내진 경우 트랜잭션 롤백으로 paymentStatus는 다시 PENDING 상태가 되어버림.
        // 상태를 좀 더 세분화하면 좋을 것 같기도..
        if (this.paymentStatus != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.paymentStatus = PaymentStatus.CANCEL_REQUESTED;
    }

    // 결제 취소 성공이랑 환불 상태 나눠야겠네요..
    public void cancelled(){
        if (this.paymentStatus != PaymentStatus.CANCEL_REQUESTED) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.paymentStatus = PaymentStatus.CANCELLED;
    }

    public void cancelFailed() {
        if (this.paymentStatus != PaymentStatus.CANCEL_REQUESTED) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.paymentStatus = PaymentStatus.CANCELLED;
    }
}
