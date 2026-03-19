package com.bootcamp.paymentdemo.user.dto;

import lombok.Getter;

@Getter
public class SignupRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
}
