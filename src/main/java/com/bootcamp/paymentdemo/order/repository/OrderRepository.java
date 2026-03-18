package com.bootcamp.paymentdemo.order.repository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

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
}