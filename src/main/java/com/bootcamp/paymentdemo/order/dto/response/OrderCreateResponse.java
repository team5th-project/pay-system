package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;

public record OrderCreateResponse(
        String orderId,
        String orderNumber,
        Long totalAmount,
        String status
) {
    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                String.valueOf(order.getId()),
                order.getOrderNumber(),
                (long) order.getTotalAmount(),
                order.getStatus().name()
        );
    }
}