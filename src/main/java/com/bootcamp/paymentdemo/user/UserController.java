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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
//
//        String email = principal.getName();
//
//        // TODO: 구현
//        // 데이터베이스에서 사용자 정보 조회
//        // customerUid 생성은 조회 한 사용자 정보로 조합하여 생성, 추천 조합 : CUST_{userId}_{rand6:난수}
//        // 임시 구현
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("email", email);
//        response.put("customerUid", "CUST_" + Math.abs(email.hashCode()));  // PortOne 고객 UID
//        response.put("name", email.split("@")[0]);  // 이메일에서 이름 추출
//        response.put("phone", "010-0000-0000");  // Kg 이니시스 전화번호 필수
//        response.put("pointBalance", 1000L);  // 포인트 잔액

        GetMyInfoResponse response= userService.getMyInfo(userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}
