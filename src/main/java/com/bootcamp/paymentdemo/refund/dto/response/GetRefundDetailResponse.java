package com.bootcamp.paymentdemo.refund.dto.response;

import com.bootcamp.paymentdemo.refund.entity.Refund;

import java.time.LocalDateTime;

public record GetRefundDetailResponse(
        Long refundId, // 환불 ID
        String orderUid, // 주문 UUID
        String paymentUid, // 결제 UUID
        Long refundAmount, // 환불 금액
        String reason, // 환불 사유
        String refundStatus, // 환불상태
        LocalDateTime requestedAt, // 환불 요청시각
        LocalDateTime refundedAt // 환불 처리시각
) {
    public static GetRefundDetailResponse from(Refund refund) {
        return new GetRefundDetailResponse(
                refund.getId(),
                refund.getPayment().getOrder().getOrderUid(),
                refund.getPayment().getPaymentUid(),
                refund.getPayment().getFinalAmount(),
                refund.getReason(),
                refund.getRefundStatus().name(),
                refund.getCreatedAt(),
                refund.getRefundedAt()
        );
    }
}
