package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;

public record OrderListResponse(
        String orderId,
        String orderNumber,
        Long totalAmount,
        String status,
        String createdAt
) {
    public static OrderListResponse from(Order order) {
        return new OrderListResponse(
                String.valueOf(order.getId()),
                order.getOrderNumber(),
                (long) order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt().toString()
        );
    }
}