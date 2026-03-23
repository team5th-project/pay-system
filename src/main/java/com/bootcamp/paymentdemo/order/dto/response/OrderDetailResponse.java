package com.bootcamp.paymentdemo.order.dto.response;

import com.bootcamp.paymentdemo.order.entity.Order;
import java.util.List;

public record OrderDetailResponse(
        String orderUid,
        String orderNumber,
        Long totalAmount,
        String status,
        List<OrderItemResponse> items,
        String createdAt,
//
//           결제 UID 추가
//
//          - PAID 상태일 때만 값이 존재, 그 외 상태(PENDING, CONFIRMED 등)는 null 반환
//          - 프론트에서 paymentUid != null && status == PAID 조건으로 결제 취소 버튼 노출 여부 판단
//          - 새로고침 후에도 주문 상세 조회 API를 통해 paymentUid를 다시 가져올 수 있어
//            결제 취소 버튼이 유지됨
//
        String paymentUid
) {
//
//      OrderDetailResponse 생성 팩토리 메서드
//
//      - paymentUid 파라미터 추가
//        -> Service 레이어에서 PAID 상태 여부 확인 후 전달
//        -> PAID 상태가 아니면 null 전달
//
//      @param order      주문 엔티티
//      @param paymentUid PAID 상태일 때 결제 UID, 나머지는 null
//
    public static OrderDetailResponse from(Order order, String paymentUid) {
        return new OrderDetailResponse(
                order.getOrderUid(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getCreatedAt().toString(),
                paymentUid
        );
    }
}