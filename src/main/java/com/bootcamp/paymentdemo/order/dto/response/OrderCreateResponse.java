package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;

public record OrderCreateResponse(
        String orderUid,     // UUID 32자리
        String orderNumber,
        Long totalAmount,
        Long usedPoint,
        Long finalAmount,
        String status
) {
    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getOrderUid(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getUsedPoint(),
                order.getFinalAmount(),
                order.getStatus().name()
        );
    }
}