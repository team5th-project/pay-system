package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.MembershipPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipPolicyRepository extends JpaRepository<MembershipPolicy, Long> {

    // 등급으로 정책 조회
    MembershipPolicy findByGrade(MembershipGrade grade);
    List<MembershipPolicy> findAllByOrderByMinAmountAsc();
}