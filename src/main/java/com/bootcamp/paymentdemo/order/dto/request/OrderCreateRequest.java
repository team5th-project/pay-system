package com.bootcamp.paymentdemo.order.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    private List<OrderItemRequest> items;  // 주문할 상품 목록

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest {
        private String productId;  // 어떤 상품인지
        private int quantity;      // 몇 개 살건지
    }
}