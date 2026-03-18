package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    // 유저별 포인트 거래내역 전체 조회
    List<PointTransaction> findByUserId(Long userId);
}