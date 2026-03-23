package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // 환불용
    List<PointTransaction> findByOrderIdAndType(Long orderId, PointType type);
    // 소멸용
    List<PointTransaction> findByTypeAndExpiredAtBefore(PointType type, LocalDateTime now);
    // 기간 기반 소멸용 - EARN 적립-만료일 사이 use 포인트 조회
    List<PointTransaction> findByUserIdAndTypeAndCreatedAtBetween(Long userId, PointType type, LocalDateTime start, LocalDateTime end);

    // 페이징
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.userId = :userId")
    Page<PointTransaction> findByUserIdWithPaging(
            @Param("userId") Long userId,
            Pageable pageable
    );
}