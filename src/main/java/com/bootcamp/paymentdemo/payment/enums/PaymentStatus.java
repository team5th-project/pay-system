package com.bootcamp.paymentdemo.payment.enums;

public enum PaymentStatus {
    PENDING,    // 대기중
    SUCCESS,    // 성공
    FAILED,     // 실패
    CANCEL_REQUESTED, // 실제 결제가 되었는데 재고 문제 등으로 결제 취소를 해야 하는 경우 등 내부 후처리 실패시 결제 취소 요청됨
    CANCEL_FAILED,  // 결제 취소 요청 보냈는데 실패함
    CANCELLED,
    REFUNDED;   // 환불 완료됨
//    EXPIRED;    // 결제 가능 시간 만료

    public static PaymentStatus from(PortOnePaymentStatus status) {
        return switch (status) {
            // TODO : 포트원 status cancel 관련된거 추가
            case READY,PAY_PENDING -> PaymentStatus.PENDING;
            case PAID -> PaymentStatus.SUCCESS;
            case FAILED -> PaymentStatus.FAILED;
            case CANCELLED -> PaymentStatus.REFUNDED;
        };
    }
}
