package com.bootcamp.paymentdemo.user.dto;

public record LoginResponse (
        String id,
        String email
){
    public static LoginResponse of (InternalLoginResponse response){
        return new LoginResponse(
                response.id().toString(),
                response.email()
        );
    }
}
