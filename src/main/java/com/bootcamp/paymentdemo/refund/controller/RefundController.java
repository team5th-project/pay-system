package com.bootcamp.paymentdemo.refund.controller;

import com.bootcamp.paymentdemo.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;
}
