package com.bootcamp.paymentdemo.order.repository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 내 주문 목록 조회
    List<Order> findByUserId(Long userId);

    // 내 주문 목록 + 상태별 조회
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    // 스케줄러용 - 7일 지난 PAID 주문 자동 확정
    // dateTime 파라미터: 현재시간 - 7일 기준값을 받습니다
    List<Order> findByStatusAndCreatedAtBefore(
            OrderStatus status,
            LocalDateTime dateTime
    );
    // 소영 추가.
    Optional<Order> findByOrderUid(String orderUid);

//    // 소영 추가
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select o from Order o where o.id = :orderId")
//    Order findByIdForUpdate(@Param("orderId") Long orderId);
}