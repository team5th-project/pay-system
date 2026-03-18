package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;

public record SignupResponse(
        Long id,
        String name,
        String customerUid,
        String email,
        String phone
) {
    public static SignupResponse of(User user){
        return new SignupResponse(
                user.getId(),
                user.getName(),
                user.getCustomerUid(),
                user.getEmail(),
                user.getPhone()
        );
    }
}
