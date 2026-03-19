package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.user.entity.User;

public record PointMeResponse(int currentPoint, MembershipGrade grade) {
    public static PointMeResponse from(User user) {
        return new PointMeResponse(
                user.getPointBalance(),
                user.getMembershipGrade()
        );
    }
}