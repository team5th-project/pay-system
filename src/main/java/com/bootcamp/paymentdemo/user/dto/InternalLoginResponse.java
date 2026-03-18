package com.bootcamp.paymentdemo.user.dto;

import com.bootcamp.paymentdemo.user.entity.User;

public record InternalLoginResponse(
        Long id,
        String email,
        String accessToken
) {
    public static InternalLoginResponse of(User user, String accessToken){
        return new InternalLoginResponse(
                user.getId(),
                user.getEmail(),
                accessToken
        );
    }
}
