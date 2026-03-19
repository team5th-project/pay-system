package com.bootcamp.paymentdemo.refund.dto.response;

import com.bootcamp.paymentdemo.refund.entity.Refund;

import java.time.LocalDateTime;

public record GetRefundDetailResponse(
        Long refundId,
        String orderUid,
        String paymentUid,
        Long refundAmount,
        String reason,
        String refundStatus,
        LocalDateTime requestedAt,
        LocalDateTime refundAt
) {
    public static GetRefundDetailResponse from(Refund refund) {
        return new GetRefundDetailResponse(
                refund.getId(),
                refund.getPayment().getOrder().getOrderUid(),
                refund.getPayment().getPaymentUid(),
                refund.getPayment().getAmount(),
                refund.getReason(),
                refund.getRefundStatus().name(),
                refund.getCreatedAt(),
                refund.getRefundedAt()
        );
    }
}
