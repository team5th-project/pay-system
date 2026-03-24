package com.bootcamp.paymentdemo.payment.respository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    boolean existsByOrderAndPaymentStatus(Order order, PaymentStatus paymentStatus);

    Optional<Payment> findByPaymentUid(String paymentUid);



    Page<Payment> findByPaymentStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    // 민교가 추가함
    // 주문 확정 시 포인트 적립을 위해 orderId로 결제 정보 조회
    // → confirmOrder() 및 스케줄러에서 finalAmount(실제 PG 결제 금액) 가져올 때 사용
    Optional<Payment> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentUid = :paymentUid")
    Optional<Payment> findByPaymentUidForUpdate(@Param("paymentUid") String paymentUid);




    @Query("""
    select p from Payment p
    where p.paymentStatus in :statuses
    order by p.modifiedAt asc
    """)
    List<Payment> findCancelablePayments(
            @Param("statuses") List<PaymentStatus> statuses,
            Pageable pageable
    );

    @Query("""
    select (count(p) > 0) from Payment p
    where p.order.id = :orderId
    and p.id <> :paymentId
    and p.paymentStatus = :status
    """)
    boolean existsSuccessfulPaymentByOrderIdExcludingPaymentId(
            @Param("orderId") Long orderId,
            @Param("paymentId") Long paymentId,
            @Param("status") PaymentStatus status
    );
}
