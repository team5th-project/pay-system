package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;
import java.util.List;

public record OrderDetailResponse(
        String orderNumber,
        Long totalAmount,
        String status,
        List<OrderItemResponse> items,
        String createdAt
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getCreatedAt().toString()
        );
    }
}