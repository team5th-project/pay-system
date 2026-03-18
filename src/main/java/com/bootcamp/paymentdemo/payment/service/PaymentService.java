package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private PaymentRepository paymentRepository;
}
