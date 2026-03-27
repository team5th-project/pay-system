package com.bootcamp.paymentdemo.refund.dto.request;

public record PortOneCancelRequest(
        String reason
) {
    public static PortOneCancelRequest of(String reason) {
        return new PortOneCancelRequest(reason);
    }
}

