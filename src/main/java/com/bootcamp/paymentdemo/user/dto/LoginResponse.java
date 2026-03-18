package com.bootcamp.paymentdemo.user.dto;

public record LoginResponse(
        String email,
        String accessToken
) {
    public static LoginResponse of(LoginRequest request, String accessToken){
        return new LoginResponse(
                request.getEmail(),
                accessToken
        );
    }
}
