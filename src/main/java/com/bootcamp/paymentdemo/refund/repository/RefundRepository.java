package com.bootcamp.paymentdemo.refund.repository;

import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByPaymentId(Long paymentId);

    /**
     * 재시도 가능한 환불 목록 조회
     *
     * - 스케줄러가 "지금 다시 시도해야 할 환불들"을 찾기 위한 조회 메서드
     *
     * [조건]
     * 1. refundStatus 가 우리가 넘긴 상태(status) 와 같아야 함
     *    -> 보통 FAILED_RETRYABLE 을 넣어서 조회
     *
     * 2. nextRetryAt 이 null 이 아니어야 함
     *    -> 다음 재시도 시각이 정해진 건만 대상
     *
     * 3. nextRetryAt <= now
     *    -> "지금 시도할 시간이 된" 건만 가져옴
     *
     * [정렬]
     * - nextRetryAt 이 빠른 순서대로 가져옴
     *   -> 먼저 재시도해야 할 건부터 처리하기 위함
     *
     * [join fetch]
     * - Refund 조회 시 연관된 Payment 도 한 번에 가져옴
     * - 이후 refund.getPayment() 할 때 추가 쿼리(N+1 방지)
     */
    @Query("""
        select r
        from Refund r
        join fetch r.payment p
        where r.refundStatus = :status
          and r.nextRetryAt is not null
          and r.nextRetryAt <= :now
        order by r.nextRetryAt asc
    """)

    List<Refund> findRetryableTargets(@Param("status") RefundStatus status,
                                      @Param("now") LocalDateTime now);

    /**
     * 환불 1건을 실제 재처리하기 전에 락을 걸고 조회
     *
     * - 스케줄러가 후보 목록에서 refundId 하나를 뽑은 뒤
     *   실제 재시도 처리 직전에 다시 조회하는 메서드
     *
     * - 목록 조회 시점과 실제 처리 시점 사이에
     *   다른 트랜잭션(웹훅, 다른 스케줄러, 수동 재처리 등)이
     *   같은 Refund 를 수정할 수 있기 때문
     *
     * [@Lock(PESSIMISTIC_WRITE)]
     * - "이 row 는 내가 수정할 예정이니 다른 트랜잭션은 기다리게함"
     *   라는 의미의 DB 쓰기 락
     *
     * [join fetch]
     * - Payment 도 같이 조회해서 처리 중 추가 쿼리 방지 (N+1 방지)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r
        from Refund r
        join fetch r.payment p
        where r.id = :refundId
    """)
    // 재시도 대상으로 뽑힌 환불 1건을 실제 처리하기 전에 락 걸고 다시 가져오는 메서드 단건조회 + 락
    Optional<Refund> findByIdForUpdate(@Param("refundId") Long refundId);
}