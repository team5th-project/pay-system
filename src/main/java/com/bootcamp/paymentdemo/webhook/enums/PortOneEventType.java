package com.bootcamp.paymentdemo.webhook.enums;

import java.util.Arrays;

public enum PortOneEventType {

    PAID("Transaction.Paid"), // 내부에서 쓸 이름 ("와부에서 들어오는 실제 문자열")
    CANCELLED("Transaction.Cancelled"),
    UNKNOWN(null);

    private final String value; // 외부 문자열 값을 저장하는 칸

    PortOneEventType(String value) {
        this.value = value;
    }

    // 문자열을 받아서 enum으로 바꿔주는 메서드
    public static PortOneEventType from(String eventType) {
        return Arrays.stream(values())
                .filter(type -> type.value != null && type.value.equals(eventType))
                .findFirst()
                .orElse(UNKNOWN);
    }

}