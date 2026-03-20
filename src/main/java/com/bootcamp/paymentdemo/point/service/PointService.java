package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.point.dto.MembershipPolicyResponse;
import com.bootcamp.paymentdemo.point.dto.MyPointResponse;
import com.bootcamp.paymentdemo.point.dto.PointHistoryResponse;
import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.MembershipPolicy;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.entity.PointType;
import com.bootcamp.paymentdemo.point.repository.MembershipPolicyRepository;
import com.bootcamp.paymentdemo.point.repository.PointTransactionRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본은 읽기 전용
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final MembershipPolicyRepository membershipPolicyRepository;
    private final UserService userService;


    /**
     * 포인트 차감
     * 결제 생성 시 PaymentService 에서 호출
     * 잔액 부족 시 INSUFFICIENT_POINT 에러 발생 후 결제 중단
     */
    @Transactional
    public void usePoint(Long userId, Long orderId, int points) {
        // 사용자 조회
        User user = userService.getUser(userId);
        // 포인트 차감
        user.deductPoint(points);
        //포인트 거래 내역 저장
        pointTransactionRepository.save(PointTransaction.use(userId, orderId, points));
    }

    /**
     * 포인트 적립
     * 주문 확정 이벤트(OrderConfirmedEvent) 수신 시 PointEventHandler 에서 호출
     * 적립 기준: 실제 PG 결제 금액 (포인트 차감 후 금액, 전액 포인트 결제 시 적립 없음)
     * paymentAmount: 포인트 차감 후 실제 PG 결제 금액(적립 기준)
     * 만료일: 적립일 기준 30일
     */
    @Transactional
    public void earnPoint(Long userId, Long orderId, Long paymentAmount) {
        // 사용자 조회
        User user = userService.getUser(userId);
        // 멤버십 등급 조회
        MembershipPolicy policy = membershipPolicyRepository.findByGrade(user.getMembershipGrade());
        // 적립 포인트 계산
        int earnedPoints = PointCalculator.calculate(paymentAmount,policy.getPointRate());
        // 전액 포인트 결제 시 적립 없음
        if (earnedPoints == 0 ) return;
        //포인트 적립
        user.addPoint(earnedPoints);
        //포인트 거래내역 저장
        pointTransactionRepository.save(
                PointTransaction.earn(userId, orderId, earnedPoints, LocalDateTime.now().plusDays(30))
        );

    }

    /**
     * 포인트 환불
     * 환불 완료 이벤트(RefundCompletedEvent) 수신 시 PointEventHandler 에서 호출
     * 해당 주문에서 사용한 포인트를 복구하고 등급 롤백
     */
    @Transactional
    public void refundPoint(Long userId, Long orderId, long refundAmount) {
        User user = userService.getUser(userId);

        List<PointTransaction> usedTransactions = pointTransactionRepository.findByOrderIdAndType(orderId, PointType.USE);
        //사용한 포인트 있는 경우만 복구
        if (!usedTransactions.isEmpty()) {
            // USE 거래 points 음수로 저장되어 있기때문에 절대값으로 복구
            int refundPoints = usedTransactions.stream()
                    .mapToInt(tx -> Math.abs(tx.getPoints()))
                    .sum();
            // 포인트 잔액 복구
            user.addPoint(refundPoints);
            // Refund 타입으로 거래내역 저장
            pointTransactionRepository.save(
                    PointTransaction.refund(userId, orderId, refundPoints));
        }
        // 누적 주문금액 차감 -> 등급 롤백 기준
        user.deductTotalOrderAmount(refundAmount);
        // 등급 롤백
        updateMembershipGrade(userId);
    }


    /**
     * 멤버십 등급 갱신
     * 결제 완료 이벤트(PaymentCompletedEvent) 수신 시 호출 → 등급 업 가능
     * 환불 완료 이벤트(RefundCompletedEvent) 수신 시 호출 → 등급 다운 가능
     * 기준: User.totalOrderAmount (누적 주문금액)
     * NORMAL(5만원 이하), VIP(10만원 이하), VVIP(10만원 초과)
     */
    @Transactional
    public void updateMembershipGrade(Long userId) {
        User user = userService.getUser(userId);
        // 전체 등급 정책 조회 후 누적금액에 맞는 등급 계산
        MembershipGrade newGrade = membershipPolicyRepository.findAllByOrderByMinAmountAsc()
                .stream()
                .filter(policy -> policy.getMaxAmount() == null ||
                        user.getTotalOrderAmount() <= policy.getMaxAmount())
                .findFirst()
                .map(MembershipPolicy::getGrade)
                .orElse(MembershipGrade.VVIP);

        user.updateGrade(newGrade);
    }


    /** 포인트 소멸
     * 스케줄러(PointExpirationScheduler)에서 호출
     * 30일 지나면 포인트 자동 소멸
     */
    @Transactional
    public void expirePoint() {
        List<PointTransaction> expiredTransactions = pointTransactionRepository.findByTypeAndExpiredAtBefore(
                PointType.EARN, LocalDateTime.now());
        // 만료된 포인트 없으면 종료
        if (expiredTransactions.isEmpty()) return;

        for (PointTransaction tx : expiredTransactions) {
            User user = userService.getUser(tx.getUserId());

            //잔액 0이면 이미 다 사용 -> 소멸 처리 스킵
            if (user.getPointBalance() <= 0) continue;
            // 잔액 있다면 남은 잔액만큼 소멸
            int expirePoints = Math.min(tx.getPoints(), user.getPointBalance());
            // 포인트 차감
            user.deductPoint(expirePoints);
            // EXPIRE 타입으로 거래내역 저장
            pointTransactionRepository.save(
                    PointTransaction.expire(tx.getUserId(), expirePoints)
            );
        }
    }

    // 현재 포인트+등급 조회 (GET /api/points/me)
    public MyPointResponse getMyPoints(Long userId) {
        User user = userService.getUser(userId);
        return MyPointResponse.from(user);
    }

    // 포인트 거래 내역 조회 (GET /api/points)
    public PointHistoryResponse getPointHistory(Long userId) {
        List<PointTransaction> transactions = pointTransactionRepository.findByUserId(userId);
        return PointHistoryResponse.from(transactions);
    }

    // 등급 정책 조회 (GET /api/points/grades)
    public List<MembershipPolicyResponse> getMembershipPolicies() {
        return membershipPolicyRepository.findAllByOrderByMinAmountAsc()
                .stream()
                .map(MembershipPolicyResponse::from)
                .toList();
    }
}