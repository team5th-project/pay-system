package com.bootcamp.paymentdemo.refund.event;

public record RefundCompletedEvent(
        Long refundId, // 이벤트에 들어가는 정보들
        Long paymentId,
        Long orderId,
        Long customerId,
        Long refundAmount
) {
}