package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.OrderItem;

public record OrderItemResponse(
        String productId,
        String productName,
        Long price,
        int quantity
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity()
        );
    }
}