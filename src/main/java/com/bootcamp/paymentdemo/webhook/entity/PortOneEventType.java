package com.bootcamp.paymentdemo.webhook.entity;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;

public enum PortOneEventType {

    PAID("Transaction.Paid"), // 내부에서 쓸 이름 ("와부에서 들어오는 실제 문자열")
    CANCELLED("Transaction.Cancelled");

    private final String value; // 외부 문자열 값을 저장하는 칸

    PortOneEventType(String value) {
        this.value = value;
    }

    // 문자열을 받아서 enum으로 바꿔주는 메서드
    public static PortOneEventType from(String eventType) {
        for (PortOneEventType type : values()) {
            // enum 안에 저장된 문자열과 웹훅에서 들어온 문자열이 같으면 그 enum을 반환
            if (type.value.equals(eventType)) {
                return type;
            }
        }
        throw new ServiceException(ErrorCode.INVALID_WEBHOOK_STATUS);
    }
}