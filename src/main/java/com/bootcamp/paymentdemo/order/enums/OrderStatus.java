package com.bootcamp.paymentdemo.order.enums;

public enum OrderStatus {
    PENDING,    // 주문 생성 후 결제 대기 중
    PAID,       // 결제 완료 (PG사 결제 성공)
    CONFIRMED,  // 주문 확정 (사용자 직접 확정 or 7일 경과 후 스케줄러 자동 확정)
    FAILED,     // 결제 실패
    REFUNDED,   // 환불 완료
    CANCELLED   // 주문 취소 (PENDING 상태에서만 가능, 결제 완료 후에는 환불 프로세스로 진행)
}
