package com.bootcamp.paymentdemo.point.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PointCalculator {

    private PointCalculator() {}

    // 포인트 적립량 계산
    // 소수점은 버림
    public static int calculate(long amount, int rate) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(rate))
                .divide(BigDecimal.valueOf(100), RoundingMode.DOWN)
                .intValue();
    }
}
