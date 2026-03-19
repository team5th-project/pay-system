package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;

import java.time.LocalDateTime;

public record PointTransactionItem(
        Long id,
        Long orderId,
        PointType type,
        int points,
        LocalDateTime createdAt,
        LocalDateTime expiredAt
        ) {
    public static  PointTransactionItem from(PointTransaction tx) {
        return new PointTransactionItem(
                tx.getId(),
                tx.getOrderId(),
                tx.getType(),
                tx.getPoints(),
                tx.getCreatedAt(),
                tx.getExpiredAt()
        );
    }
}
