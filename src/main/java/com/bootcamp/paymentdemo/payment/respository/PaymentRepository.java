package com.bootcamp.paymentdemo.payment.respository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {

    Optional<Payment> findByPaymentUid(String paymentUid);

    // 주문 확정 시 포인트 적립을 위해 orderId로 결제 정보 조회
    // → confirmOrder() 및 스케줄러에서 finalAmount(실제 PG 결제 금액) 가져올 때 사용
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByOrderIdAndPaymentStatus(Long orderId, PaymentStatus paymentStatus);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentUid = :paymentUid")
    Optional<Payment> findByPaymentUidForUpdate(@Param("paymentUid") String paymentUid);

    boolean existsByOrderAndPaymentStatus(Order order, PaymentStatus paymentStatus);

    // 결제 스케줄러에서 사용할 내용

    // 1. PENDING
    // PENDING 상태인 결제 건 중 만료 시간이 지난 결제들을 오래된 순으로 조회
    List<Payment> findByPaymentStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentStatus status,
            LocalDateTime now,
            Pageable pageable
    );
    // 2. CANCEL 재처리
    List<Payment> findByPaymentStatusInAndModifiedAtBeforeOrderByIdAsc(
            Collection<PaymentStatus> statuses,
            LocalDateTime time,
            Pageable pageable
    );

}
