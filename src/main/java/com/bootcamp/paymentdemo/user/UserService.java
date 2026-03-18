package com.bootcamp.paymentdemo.user;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.refreshtoken.RefreshToken;
import com.bootcamp.paymentdemo.security.refreshtoken.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
//    private final MembershipService membershipService;

    // 1. login
    @Transactional
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                ()-> new ServiceException(ErrorCode.TEMP_ERROR)
        );
        if(!request.getPassword().equals(user.getPassword())){
            throw new ServiceException(ErrorCode.WRONG_PASSWORD);
        }

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new ServiceException(ErrorCode.TEMP_ERROR);
        }

        // access token 과 refresh 토큰을 각각 발급하고
        // refresh token 은 repository 에 저장, access token 은 header 로 반환
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getUserRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

//        refresh token 저장 ... 로직
        RefreshToken tokenEntity = refreshTokenRepository.findById(user.getId())
                .orElse(new RefreshToken(user.getId(), refreshToken));

        refreshTokenRepository.save(tokenEntity);
        // 토큰 파싱 로직

        return LoginResponse.of(request);
    }

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {
//        String customerUid = "CUST_" + UUID.randomUUID().toString().substring(0,8);
        // CUST_XXXXXXXX 의 customerUid 생성하기
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = userRepository.save(
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .phone(request.getPhone())
                        .build());

//        membershipService.createMembership(user.getId());

        return SignupResponse.of(user);
    }

    // 현재 로그인 한 사용자의 정보 출력
    public GetMyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new ServiceException(ErrorCode.USER_NOT_FOUND)
        );

        return GetMyInfoResponse.of(user);

    }

    //refresh token 재발급 로직
    @Transactional
    public String reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)){
            throw new ServiceException(ErrorCode.TEMP_ERROR);
        }

        Long userId = Long.valueOf(jwtTokenProvider.getId(refreshToken));
        User user = userRepository.findById(userId).orElseThrow(
                ()->new ServiceException(ErrorCode.USER_NOT_FOUND)
        );

        RefreshToken savedToken = refreshTokenRepository.findById(userId).orElseThrow(
                ()-> new ServiceException(ErrorCode.TEMP_ERROR));

        if(!savedToken.getToken().equals(refreshToken)){
            throw new ServiceException(ErrorCode.TEMP_ERROR);
        }

        return jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getUserRole().toString());
    }


}
