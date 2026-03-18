package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private int totalOrderAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipGrade membershipGrade;

    @Column(nullable = false)
    private int currentPoint;

    @Builder
    private Membership(Long userId, int totalOrderAmount,
                       MembershipGrade membershipGrade, int currentPoint) {
        this.userId = userId;
        this.totalOrderAmount = totalOrderAmount;
        this.membershipGrade = membershipGrade;
        this.currentPoint = currentPoint;
    }

    // 회원가입 시 최초 생성
    public static Membership create(Long userId) {
        return Membership.builder()
                .userId(userId)
                .totalOrderAmount(0)
                .membershipGrade(MembershipGrade.NORMAL)
                .currentPoint(0)
                .build();
    }

    // 등급 갱신
    public void updateGrade(int totalOrderAmount, MembershipGrade membershipGrade) {
        this.totalOrderAmount = totalOrderAmount;
        this.membershipGrade = membershipGrade;
    }

    // 포인트 갱신
    public void updatePoint(int currentPoint) {
        this.currentPoint = currentPoint;
    }
}