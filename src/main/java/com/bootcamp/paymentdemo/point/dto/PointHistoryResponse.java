package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import java.util.List;

public record PointHistoryResponse(
        List<PointTransactionItem> pointHistories
) {
    // TODO: 현재 전체 조회 방식으로 데이터가 많아지면 서버 부하 발생 가능
    //       추후 팀 협의 후 페이징 처리 예정
    public static PointHistoryResponse from(List<PointTransaction> transactions) {
        return new PointHistoryResponse(
                transactions.stream()
                        .map(PointTransactionItem::from)
                        .toList()
        );
    }
}