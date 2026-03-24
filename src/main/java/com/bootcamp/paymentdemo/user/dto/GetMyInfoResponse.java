package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;

public record GetMyInfoResponse(
        String id,
        String email,
        String name,
        String phone,
        String totalAmount,
        String rank,
        String availablePoint
) {
    public static GetMyInfoResponse of(User user, int availablePoint){
        return new GetMyInfoResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getTotalOrderAmount().toString(),
                user.getMembershipGrade().toString(),
                String.valueOf(availablePoint)
        );
    }
}
