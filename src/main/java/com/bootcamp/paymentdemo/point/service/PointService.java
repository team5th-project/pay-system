package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.point.dto.MembershipPolicyResponse;
import com.bootcamp.paymentdemo.point.dto.PointHistoryResponse;
import com.bootcamp.paymentdemo.point.dto.PointMeResponse;
import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.MembershipPolicy;
import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.repository.MembershipPolicyRepository;
import com.bootcamp.paymentdemo.point.repository.PointTransactionRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본은 읽기 전용
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final MembershipPolicyRepository membershipPolicyRepository;
    private final UserService userService;

    // 포인트 차감
    /**
     * 포인트 차감
     * 결제 생성 시 PaymentService 에서 호출
     * 잔액 부족 시 INSUFFICIENT_POINT 에러 발생 후 결제 중단
     */
    @Transactional
    public void usePoint(Long userId, Long orderId, int points) {
        // 사용자 조회
        User user = userService.getUser(userId);
        // 포인트 차감(잔액 부족시 INSUFFICIENt_POINTS 에러 발생)
        user.deductPoint(points);
        //포인트 거래 내역 저장
        pointTransactionRepository.save(PointTransaction.use(userId, orderId, points));
    }

    // 포인트 적립
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
        // 적립 포인트 게산
        int earnedPoints = (int) (paymentAmount * policy.getPointRate() / 100.0);
        // 전액 포인트 결제 시 적립 없음
        if (earnedPoints == 0 ) return;
        //포인트 적립
        user.addPoint(earnedPoints);
        //포인트 거래내역 저장
        pointTransactionRepository.save(
                PointTransaction.earn(userId, orderId, earnedPoints, LocalDateTime.now().plusDays(30))
        );

    }

    // 등급 갱신
    /**
     * 멤버십 등급 갱신
     * 결제 완료 이벤트(PaymentCompletedEvent) 수신 시 호출 → 등급 업 가능
     * 환불 완료 이벤트(RefundCompletedEvent) 수신 시 호출 → 등급 다운 가능
     * 기준: User.totalOrderAmount (누적 주문금액)
     * NORMAL(5만원 이하), VIP(10만원 이하), VVIP(15만원 이상)
     */
    @Transactional
    public void updateMembershipGrade(Long userId) {
        User user = userService.getUser(userId);
        // 전체 등급 정책 조회 후 누적금액에 맞는 등급 계산
        MembershipGrade newGrade = membershipPolicyRepository.findAll()
                .stream()
                .sorted(Comparator.comparingLong(MembershipPolicy::getMinAmount))
                .filter(policy -> policy.getMaxAmount() == null ||
                        user.getTotalOrderAmount() <= policy.getMaxAmount())
                .findFirst()
                .map(MembershipPolicy::getGrade)
                .orElse(MembershipGrade.VVIP);

        user.updateGrade(newGrade);
    }

    // 현재 포인트+등급 조회 (GET /api/points/me)
    public PointMeResponse getMyPoints(Long userId) {
        User user = userService.getUser(userId);
        return PointMeResponse.from(user);
    }

    // 포인트 거래 내역 조회 (GET /api/points)
    public PointHistoryResponse getPointHistory(Long userId) {
        List<PointTransaction> transactions = pointTransactionRepository.findByUserId(userId);
        return PointHistoryResponse.from(transactions);
    }

    // 등급 정책 조회 (GET /api/points/grades)
    public List<MembershipPolicyResponse> getMembershipPolicies() {
        return membershipPolicyRepository.findAll()
                .stream()
                .map(MembershipPolicyResponse::from)
                .toList();
    }
}