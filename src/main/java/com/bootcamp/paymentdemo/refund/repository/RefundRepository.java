package com.bootcamp.paymentdemo.refund.repository;

import com.bootcamp.paymentdemo.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByPaymentPaymentUid(String paymentUid);

    Optional<Refund> findByPaymentId(Long paymentId);
}
