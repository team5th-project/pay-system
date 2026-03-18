package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.point.repository.MembershipPolicyRepository;
import com.bootcamp.paymentdemo.point.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본은 읽기 전용
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final MembershipPolicyRepository membershipPolicyRepository;

    // 포인트 적립
    @Transactional
    public void earnPoint() {}

    // 포인트 차감
    @Transactional
    public void usePoint() {}

    // 포인트 조회 - readOnly 그대로
    public void getPointHistory() {}

    // 등급 갱신
    @Transactional
    public void updateMembershipGrade() {}

    // 등급 조회 - readOnly 그대로
    public void getMembershipGrade() {}
}