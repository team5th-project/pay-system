package com.bootcamp.paymentdemo.refund.dto.response;

import com.bootcamp.paymentdemo.refund.entity.Refund;

import java.time.LocalDateTime;

//환불번호
//주문번호 또는 결제번호
//환불금액
//환불상태
//환불요청일시
public record GetRefundListResponse(
        Long refundId,
        String orderUid,
        String paymentUid,
        Long refundAmount,
        String refundStatus,
        LocalDateTime refundedAt
) {
    public static GetRefundListResponse from(Refund refund) {
        return new GetRefundListResponse(
                refund.getId(),
                refund.getPayment().getOrder().getOrderUid(),
                refund.getPayment().getPaymentUid(),
                refund.getPayment().getFinalAmount(),
                refund.getRefundStatus().name(),
                refund.getRefundedAt()
        );
    }
}
