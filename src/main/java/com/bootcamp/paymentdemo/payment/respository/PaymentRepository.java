package com.bootcamp.paymentdemo.payment.respository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    boolean existsByOrderAndPaymentStatus(Order order, PaymentStatus paymentStatus);
}
