package com.bootcamp.paymentdemo.webhook.enums;

import java.util.Arrays;
import java.util.Optional;

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

    // TODO : 공통으로 사용해야 할 파일이라 일단 코드리뷰 받기 전까지는 기존 코드 살려두려고 이름을 from_ 으로 사용중입니다. 둘 중 하나만 남겨서 사용하면 됩니다.
    // 소영 추가. 미지원 이벤트면 예외 말고 무시
    public static Optional<PortOneEventType> from_(String eventType) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(eventType))
                .findFirst();
    }
}