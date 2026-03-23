package com.bootcamp.paymentdemo.point.dto;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.UserPoint;
import com.bootcamp.paymentdemo.user.entity.User;

public record MyPointResponse(int currentPoint, MembershipGrade grade) {
    public static MyPointResponse from(User user, UserPoint userPoint) {

        return new MyPointResponse(
                userPoint.getTotalPoint() - userPoint.getUsedPoint() - userPoint.getHeldPoint(),
                user.getMembershipGrade()
        );
    }
}