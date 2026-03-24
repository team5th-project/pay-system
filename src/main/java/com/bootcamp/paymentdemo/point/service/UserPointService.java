package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.point.dto.MembershipPolicyResponse;
import com.bootcamp.paymentdemo.point.dto.MyPointResponse;
import com.bootcamp.paymentdemo.point.dto.PointTransactionItem;
import com.bootcamp.paymentdemo.point.entity.*;
import com.bootcamp.paymentdemo.point.repository.MembershipPolicyRepository;
import com.bootcamp.paymentdemo.point.repository.UserPointRepository;
import com.bootcamp.paymentdemo.point.repository.PointTransactionRepository;
import com.bootcamp.paymentdemo.user.UserService;
import com.bootcamp.paymentdemo.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPointService {

    // point 를 사용하거나, rollback 하거나, point 를 추가하는 로직
    // 1. point 가점유
    // 2. point 가 실제로 사용됨 + 가점유 해제
    // 3. point 가 실제로 사용되지 않음 (결제 취소) + 가점유 해제
    // 4. 주문 확정으로 total point 추가
    // 5, 주문 확정 전 환불 요청으로 사용된 포인트... 를 다시 돌려놓음

    private final UserPointRepository pointRepository;
    private final MembershipPolicyRepository membershipPolicyRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserService userService;

    /**
     * 1
     * 포인트를 사용하려고 할 때 가점유 걸기
     * 결제 생성 시 PaymentService 에서 호출
     * 잔액 부족 시 INSUFFICIENT_POINT 에러 발생 후 결제 중단
     */
    @Transactional
    public void holdPoint(Long userId, Long orderId, int points) {
        // 사용자의 포인트 조회
        UserPoint userPoint = pointRepository.findByUserIdForUpdate(userId);

        // 포인트 가점유
        userPoint.hold(points);
    }

    /**
     * 2
     * 실제로 포인트가 사용됨
     * 주문 확정 시 PaymentService 에서 호출
     * 잔액 부족 시 INSUFFICIENT_POINT 에러 발생 후 결제 중단
     */
    @Transactional
    public void usePoint(Long userId, Long orderId, int points) {
        // 사용자 조회
        UserPoint userPoint = pointRepository.findByUserIdForUpdate(userId);
        // 포인트 차감
        userPoint.commit(points);
        //포인트 거래 내역 저장
        saveTransaction(PointTransaction.use(userId, orderId, points));
    }

    /**
     * 3
     * 결제가 취소되어 가점유 상태가 해제
     * 결제 실패 또는 취소 시 PaymentService 에서 호출
     */
    @Transactional
    public void cancelUsePoint(Long userId, Long orderId, int points){
        // 사용자 조회
        UserPoint userPoint = pointRepository.findByUserIdForUpdate(userId);
        userPoint.release(points);
    }


    /**
     * 4
     * 포인트 적립
     * 주문 확정 이벤트(OrderConfirmedEvent) 수신 시 PointEventHandler 에서 호출
     * 적립 기준: 실제 PG 결제 금액 (포인트 차감 후 금액, 전액 포인트 결제 시 적립 없음)
     * paymentAmount: 포인트 차감 후 실제 PG 결제 금액(적립 기준)
     * 만료일: 적립일 기준 30일
     */
    @Transactional
    public void earnPoint(Long userId, Long orderId, Long paymentAmount) {
        // 사용자 조회
        UserPoint userPoint = pointRepository.findByUserIdForUpdate(userId);
        User user = userService.getUser(userId);
        // 멤버십 등급 조회
        MembershipPolicy policy = membershipPolicyRepository.findByGrade(user.getMembershipGrade());
        // 적립 포인트 계산
        int earnedPoints = PointCalculator.calculate(paymentAmount,policy.getPointRate());
        // 전액 포인트 결제 시 적립 없음
        if (earnedPoints == 0 ) return;
        //포인트 적립
        userPoint.addPoint(earnedPoints);
        //포인트 거래내역 저장
        saveTransaction(PointTransaction.earn(userId, orderId, earnedPoints, LocalDateTime.now().plusDays(30)));
    }

    /**
     * 5
     * 포인트 환불
     * 환불 완료 이벤트(RefundCompletedEvent) 수신 시 PointEventHandler 에서 호출
     * 해당 주문에서 사용한 포인트를 복구하고 등급 롤백
     */
    @Transactional
    public void refundPoint(Long userId, Long orderId, long refundAmount) {
        UserPoint userPoint = pointRepository.findByUserIdForUpdate(userId);
        User user = userService.getUser(userId);
        List<PointTransaction> usedTransactions = pointTransactionRepository.findByOrderIdAndType(orderId, PointType.USE);
        //사용한 포인트 있는 경우만 복구
        int refundPoints = 0;
        if (!usedTransactions.isEmpty()) {
            // USE 거래 points 음수로 저장되어 있기때문에 절대값으로 복구
            refundPoints = usedTransactions.stream()
                    .mapToInt(tx -> Math.abs(tx.getPoints()))
                    .sum();

            userPoint.restoreUsedPoint(refundPoints);
            // Refund 타입으로 거래내역 저장
            saveTransaction(PointTransaction.refund(userId, orderId, refundPoints));
        }
        // 누적 주문금액 차감 -> 등급 롤백 기준
        user.deductTotalOrderAmount(refundAmount);
        // 등급 롤백
        updateMembershipGrade(userId);
    }


    /**
     * 멤버십 등급 갱신
     * 기준: User.totalOrderAmount (누적 주문금액)
     * NORMAL(5만원 이하), VIP(10만원 이하), VVIP(10만원 초과)
     */
    @Transactional
    public void updateMembershipGrade(Long userId) {
        User user = userService.getUser(userId);
        List<MembershipPolicy> policies = membershipPolicyRepository.findAllByOrderByMinAmountAsc();
        // 정책 데이터 없을 경우 에러 던짐
        if (policies.isEmpty()) {
            throw new ServiceException(ErrorCode.MEMBERSHIP_POLICY_NOT_FOUND);
        }

        // 전체 등급 정책 조회 후 누적금액에 맞는 등급 계산
        MembershipGrade newGrade = policies.stream()
                .filter(policy -> policy.getMaxAmount() == null ||
                        user.getTotalOrderAmount() <= policy.getMaxAmount())
                .findFirst()
                .map(MembershipPolicy::getGrade)
                .orElseThrow(() -> new ServiceException(ErrorCode.MEMBERSHIP_POLICY_NOT_FOUND));
        user.updateGrade(newGrade);
    }

    /** 포인트 소멸
     * 스케줄러(PointExpirationScheduler)에서 호출
     * 30일 지나면 포인트 자동 소멸
     *
     * 현재 방식: 기간 기반 소멸
     * 소멸 포인트 = 만료될 EARN - 적립~만료일 사이 USE 포인트
     *
     * TODO: 한계점 - 두 EARN의 기간 겹치는 구간 use 있으면 같은 use가 두 EARN 계산에 중복 포함될 수 있음
     *       FIFO같은 방식 개선 필요
     */
    @Transactional
    public void expirePoint() {
        List<PointTransaction> expiredTransactions = pointTransactionRepository.findByTypeAndExpiredAtBefore(
                PointType.EARN, LocalDateTime.now());
        // 만료된 포인트 없으면 종료
        if (expiredTransactions.isEmpty()) return;

        for (PointTransaction tx : expiredTransactions) {
            UserPoint userPoint = pointRepository.findByUserIdForUpdate(tx.getUserId());
            // 해당 EARN 적립-만료일 사이 USE포인트 합산
            List<PointTransaction> usedTransactions = pointTransactionRepository.findByUserIdAndTypeAndCreatedAtBetween(
                    tx.getUserId(),
                    PointType.USE,
                    tx.getCreatedAt(),  //적립일
                    tx.getExpiredAt() // 만료일
            );

            int totalUsed = usedTransactions.stream().mapToInt(
                    t -> Math.abs(t.getPoints())).sum();

            // 소멸 포인트 = EARN - 기간내 use (음수면 0)
            int expirePoints = Math.max(0, tx.getPoints() - totalUsed);

            //소멸포인트 없으면 스킵
            if (expirePoints <= 0) continue;
            // 포인트 차감
            userPoint.expire(expirePoints);
            // EXPIRE 타입으로 거래내역 저장
            pointTransactionRepository.save(
                    PointTransaction.expire(tx.getUserId(), expirePoints)
            );
        }
    }

    /** 포인트 지급
     *
     * order 없이 직접 호출하여 포인트를 지급하는 경우
     * orderId == null
     */
    @Transactional
    public void grantPoint(Long userId, int points){
        UserPoint userPoint = pointRepository.findByUserIdForUpdate(userId);
        userPoint.addPoint(points);

        pointTransactionRepository.save(PointTransaction.grant(userId, points));
    }

    // 현재 포인트+등급 조회 (GET /api/points/me)
    public MyPointResponse getMyPoints(Long userId) {
        UserPoint userPoint = pointRepository.findByUserId(userId);
        User user = userService.getUser(userId);
        return MyPointResponse.from(user, userPoint);
    }

    // 등급 정책 조회 (GET /api/points/grades)
    public List<MembershipPolicyResponse> getMembershipPolicies() {
        return membershipPolicyRepository.findAllByOrderByMinAmountAsc()
                .stream()
                .map(MembershipPolicyResponse::from)
                .toList();
    }

    public void saveTransaction(PointTransaction transaction) {
        pointTransactionRepository.save(transaction);
    }

    // 포인트 거래 내역 조회 (GET /api/points)
    public Page<PointTransactionItem> getPointHistory(Long userId, Pageable pageable) {
        return pointTransactionRepository.findByUserIdWithPaging(userId, pageable).map(PointTransactionItem::from);
    }

}
