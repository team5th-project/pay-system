package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;


// 지금 당장 없어도 `OrderCreateResponse` 를 그대로 써도 작동은 합니다.
////주문 확정 → 사용자 보여질 정보가 달라 미리 만들었습니다.
//예를 들어 나중에:
// - 적립된 포인트
//- 확정 시간
//- 등 추가될 수 있어서 미리 분리하였습니다.


    public record OrderConfirmResponse(
            String orderUid,  //UUID 32자리
            String orderNumber, // TSID (사용자 노출용)
            String status
    ) {
        public static OrderConfirmResponse from(Order order) {
            return new OrderConfirmResponse(
                    order.getOrderUid(),
                    order.getOrderNumber(),
                    order.getStatus().name()
            );
        }
    }

