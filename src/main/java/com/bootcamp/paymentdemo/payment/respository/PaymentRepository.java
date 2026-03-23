package com.bootcamp.paymentdemo.payment.respository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    boolean existsByOrderAndPaymentStatus(Order order, PaymentStatus paymentStatus);

    Optional<Payment> findByPaymentUid(String paymentUid);

    // 민교가 추가함
    // 주문 확정 시 포인트 적립을 위해 orderId로 결제 정보 조회
    // → confirmOrder() 및 스케줄러에서 finalAmount(실제 PG 결제 금액) 가져올 때 사용
    Optional<Payment> findByOrderId(Long orderId);
}
