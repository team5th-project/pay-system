package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;

public record GetMyInfoResponse(
        Long id,
        String email,
        String name,
        String phone
) {
    public static GetMyInfoResponse of(User user){
        return new GetMyInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone()
        );
    }
}
