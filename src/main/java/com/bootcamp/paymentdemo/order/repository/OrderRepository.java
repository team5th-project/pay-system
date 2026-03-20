package com.bootcamp.paymentdemo.order.repository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<Order> findByOrderUid(String orderUid);

//
//      내 주문 목록 페이징 조회
//
//      - ORDER BY를 @Query에 고정하지 않고 Pageable에 위임
//        → 클라이언트가 ?sort=createdAt,desc 또는 ?sort=totalAmount,asc 등
//          원하는 방향을 동적으로 지정할 수 있음 (동적 정렬)
//      - 아무 sort 파라미터도 안 보내면 Controller의 @PageableDefault 기본값이 적용됨
//
//      @param userId   조회할 유저 ID
//      @param pageable 페이지 번호·사이즈·정렬 정보 (Spring이 자동 주입)
//      @return 페이징된 Order 목록
//
    @Query("SELECT o FROM Order o WHERE o.userId = :userId")
    Page<Order> findByUserIdWithPaging(@Param("userId") Long userId, Pageable pageable);
}