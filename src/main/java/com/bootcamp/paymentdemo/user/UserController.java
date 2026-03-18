package com.bootcamp.paymentdemo.user;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    /**
     * 로그인 API
     * POST /api/auth/login
     *
     * 요청 본문:
     * {
     *   "email": "user@example.com",
     *   "password": "password123"
     * }
     *
     * 응답 헤더:
     * Authorization: Bearer eyJhbGc...
     *
     * 응답 본문:
     * {
     *   "success": true,
     *   "email": "user@example.com"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    /**
     * 회원가입 API
     * POST /api/auth/signup
     *
     * 요청 본문:
     * {
     *   "name": tako,
     *   "email": "tako@example.com",
     *   "password": "password123",
     *   "phone": 010-0000-0000
     * }
     *
     * 응답 본문:
     * {
     *   "success": true,
     *   "id": 1,
     *   "name": tako,
     *   "customerUid": CUST_XXXXXXXX" (X 는 랜덤)
     *   "email": "user@example.com",
     *   "phone": 010-0000-0000
     * }
     */
    @PostMapping("/signup")
    ResponseEntity<CommonResponse<SignupResponse>> signup(@RequestBody SignupRequest request){
        SignupResponse response = userService.signup(request);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회 API
     * GET /api/auth/me
     *
     * 응답:
     * {
     *   "success": true,
     *   "email": "user@example.com",
     *   "customerUid": "CUST_XXXXXXXX",
     *   "name": "홍길동",
     *   "phone": 010-0000-0000
     *   "pointBalance": 5000
     * }
     *
     * 중요: customerUid는 PortOne 빌링키 발급 시 활용!
     */
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<GetMyInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        GetMyInfoResponse response= userService.getMyInfo(userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<CommonResponse<String>> reissue(
            @RequestHeader String refreshToken){

        String response = userService.reissue(refreshToken);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

}
