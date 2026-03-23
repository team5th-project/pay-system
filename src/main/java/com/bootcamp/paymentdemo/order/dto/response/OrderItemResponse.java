package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.OrderItem;

public record OrderItemResponse(
        Long productId,    // String → Long 변경 (Product PK 타입에 맞춤, #101)
        String productName,
        Long price,
        int quantity
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),  // getProductId() -> getProduct().getId() 변경 (#101)
                item.getProductName(),
                item.getPrice(),
                item.getQuantity()
        );
    }
}