package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.MembershipPolicy;

public record MembershipPolicyResponse(
        MembershipGrade grade,
        long minAmount,
        Long maxAmount,
        int pointRate) {
    public static MembershipPolicyResponse from(MembershipPolicy policy) {
        return new MembershipPolicyResponse(
                policy.getGrade(),
                policy.getMinAmount(),
                policy.getMaxAmount(),
                policy.getPointRate()
        );
    }
}
