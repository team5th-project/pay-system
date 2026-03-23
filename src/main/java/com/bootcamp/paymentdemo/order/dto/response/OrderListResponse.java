package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;

public record OrderListResponse(
        String orderUid,
        String orderNumber,
        Long totalAmount,
        Long usedPoint,
        String status,
        String createdAt
) {
    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                order.getOrderUid(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getUsedPoint(),
                order.getStatus().name(),
                order.getCreatedAt().toString()
        );
    }
}