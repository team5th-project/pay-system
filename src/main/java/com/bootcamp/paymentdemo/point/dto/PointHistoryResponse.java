package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import java.util.List;

public record PointHistoryResponse(
        List<PointTransactionItem> pointHistories
) {
    public static PointHistoryResponse from(List<PointTransaction> transactions) {
        return new PointHistoryResponse(
                transactions.stream()
                        .map(PointTransactionItem::from)
                        .toList()
        );
    }
}