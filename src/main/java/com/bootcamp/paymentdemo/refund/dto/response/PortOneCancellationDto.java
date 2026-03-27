package com.bootcamp.paymentdemo.refund.dto.response;
import com.bootcamp.paymentdemo.refund.enums.PortOneRefundStatus;
import java.time.OffsetDateTime;

public record PortOneCancellationDto(
        PortOneRefundStatus status,
        String id,
        Long totalAmount,
        String reason,
        OffsetDateTime requestedAt,
        OffsetDateTime cancelledAt // JSON → DTO 변환 시 자동 파싱됨
) {
}
