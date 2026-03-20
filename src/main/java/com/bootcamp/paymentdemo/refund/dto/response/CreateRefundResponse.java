package com.bootcamp.paymentdemo.refund.dto.response;

import com.bootcamp.paymentdemo.refund.entity.Refund;

import java.time.LocalDateTime;

public record CreateRefundResponse(
        Long refundId,
        String orderUid,
        String paymentUid,
        Long refundAmount,
        String reason,
        String refundStatus,
        LocalDateTime refundedAt
) {
    public static CreateRefundResponse from(Refund refund) {
        return new CreateRefundResponse(
                refund.getId(), // 환불 고유 ID
                refund.getPayment().getOrder().getOrderUid(), // 주문번호 UUID
                refund.getPayment().getPaymentUid(), // 결제번호 UUID
                refund.getPayment().getAmount(), // 결제 총 금액 = 환불 금액
                refund.getReason(), // 환불 사유
                refund.getRefundStatus().name(), // 환불 상태
                refund.getRefundedAt() // 환불 처리시각
        );
    }
}