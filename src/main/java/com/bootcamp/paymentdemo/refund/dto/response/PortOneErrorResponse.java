package com.bootcamp.paymentdemo.refund.dto.response;

public record PortOneErrorResponse(
        String type,
        String message,
        String pgCode,
        String pgMessage
) {}