package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "membership_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MembershipPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private MembershipGrade grade;

    // 해당 등급급의 최소 결제 금액 기준
    @Column(nullable = false)
    private int minAmount;

    // 해당 등급의 최대 결제 금액 기준(VVIP는 상한선 없어서 NULL)
    @Column
    private Integer maxAmount;

    @Column(nullable = false)
    private int pointRate;

    @Builder
    private MembershipPolicy(MembershipGrade grade, int minAmount,
                             Integer maxAmount, int pointRate) {
        this.grade = grade;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.pointRate = pointRate;
    }

    public static MembershipPolicy create(MembershipGrade grade, int minAmount,
                                          Integer maxAmount, int pointRate) {
        return MembershipPolicy.builder()
                .grade(grade)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .pointRate(pointRate)
                .build();
    }
}