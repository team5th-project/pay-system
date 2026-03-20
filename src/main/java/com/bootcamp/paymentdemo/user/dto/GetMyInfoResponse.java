package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;

public record GetMyInfoResponse(
        String email,
        String customerUid,
        String name,
        String phone
) {
    public static GetMyInfoResponse of(User user){
        return new GetMyInfoResponse(
                user.getEmail(),
                user.getCustomerUid(),
                user.getName(),
                user.getPhone()
        );
    }
}
