package com.bootcamp.paymentdemo.order.repository;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

//    // 소영 추가
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select o from Order o where o.id = :orderId")
//    Order findByIdForUpdate(@Param("orderId") Long orderId);
    /**
     * 내 주문 목록 페이징 + 상태 필터 조회
     *
     * - status가 null이면 전체 조회, 값이 있으면 해당 상태만 필터링
     *   → Controller에서 @RequestParam(required = false)로 받아 선택적으로 전달
     * - ORDER BY를 @Query에 고정하지 않고 Pageable에 위임 (동적 정렬)
     *   → 클라이언트가 ?sort=createdAt,desc 또는 ?sort=totalAmount,asc 등 자유롭게 지정 가능
     * - 아무 sort 파라미터도 안 보내면 Controller의 @PageableDefault 기본값이 적용됨
     *
     * @param userId   조회할 유저 ID
     * @param status   필터링할 주문 상태 (null 이면 전체 조회)
     * @param pageable 페이지 번호·사이즈·정렬 정보 (Spring이 자동 주입)
     * @return 페이징된 Order 목록
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND (:status IS NULL OR o.status = :status)")
    Page<Order> findByUserIdWithPaging(@Param("userId") Long userId,
                                       @Param("status") OrderStatus status,
                                       Pageable pageable);
}