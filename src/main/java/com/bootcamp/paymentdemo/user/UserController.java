package com.bootcamp.paymentdemo.user;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import com.bootcamp.paymentdemo.user.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    /**
     * 로그인 API
     * POST /api/auth/login
     * <p>
     * 요청 본문:
     * {
     * "email": "user@example.com",
     * "password": "password123"
     * }
     * <p>
     * 응답 헤더:
     * Authorization: Bearer eyJhbGc...
     * <p>
     * 응답 본문:
     * {
     * "success": true,
     * "email": "user@example.com"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletResponse response) {

        InternalLoginResponse internalResponse = userService.login(request);
        // access 토큰은 헤더에 넣어서 주고
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + internalResponse.accessToken());

        // access 토큰을 뺀 정보만 새로 response 로 만들어서 던지기
        LoginResponse responseDto = LoginResponse.of(internalResponse);

        return CommonResponseHandler.success(HttpStatus.OK, responseDto, headers);
    }

    /**
     * 회원가입 API
     * POST /api/auth/signup
     * <p>
     * 요청 본문:
     * {
     * "name": tako,
     * "email": "tako@example.com",
     * "password": "password123",
     * "phone": 010-0000-0000
     * }
     * <p>
     * 응답 본문:
     * {
     * "success": true,
     * "id": 1,
     * "name": tako,
     * "customerUid": CUST_XXXXXXXX" (X 는 랜덤)
     * "email": "user@example.com",
     * "phone": 010-0000-0000
     * }
     */
    @PostMapping("/signup")
    ResponseEntity<CommonResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {
        log.info("controller 진입 성공");
        SignupResponse response = userService.signup(request);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    /**
     * 현재 로그인한 사용자 정보 조회 API
     * GET /api/auth/me
     * <p>
     * 응답:
     * {
     * "success": true,
     * "email": "user@example.com",
     * "customerUid": "CUST_XXXXXXXX",
     * "name": "홍길동",
     * "phone": 010-0000-0000
     * "pointBalance": 5000
     * }
     * <p>
     * 중요: customerUid는 PortOne 빌링키 발급 시 활용!
     */
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<GetMyInfoResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        GetMyInfoResponse response = userService.getMyInfo(userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    @PostMapping("/reissue")
    public ResponseEntity<CommonResponse<Void>> reissue(
            @RequestHeader String refreshToken) {

        String token = userService.reissue(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return CommonResponseHandler.success(HttpStatus.OK, null, headers);
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @RequestHeader("Authorization") String accessToken) {

        userService.logout(accessToken);

        return CommonResponseHandler.success(HttpStatus.OK);
    }


}
