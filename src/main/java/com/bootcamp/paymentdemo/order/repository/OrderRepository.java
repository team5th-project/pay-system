package com.bootcamp.paymentdemo.order.repository;

import com.bootcamp.paymentdemo.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderItem, Long> {

    // 특정 주문의 상품 목록 조회
    List<OrderItem> findByOrderId(Long orderId);
}
