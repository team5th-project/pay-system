package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.user.entity.User;

public record MyPointResponse(int currentPoint, MembershipGrade grade) {
    public static MyPointResponse from(User user) {
        return new MyPointResponse(
                user.getPointBalance(),
                user.getMembershipGrade()
        );
    }
}