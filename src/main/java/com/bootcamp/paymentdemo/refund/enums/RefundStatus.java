package com.bootcamp.paymentdemo.refund.enums;

public enum RefundStatus {
    REQUESTED, // 환불 엔티티 생성
    COMPLETED, // 최종 성공
    PROCESSING, // 포트원에 환불 요청 진행중
    FAILED_RETRYABLE, // 실패(재시도 예정) 자동 재시도 대상 실패 상태
    FAILED_FINAL // 재시도 불가 또는 재시도 한도초과 상태(관리자 확인 필요)
}
