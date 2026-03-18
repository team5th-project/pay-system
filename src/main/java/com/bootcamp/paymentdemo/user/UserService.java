package com.bootcamp.paymentdemo.user;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.refreshtoken.RefreshToken;
import com.bootcamp.paymentdemo.security.refreshtoken.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.dto.*;
import lombok.RequiredArgsConstructor;
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

    // 1. login
    @Transactional
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                ()-> new ServiceException(ErrorCode.TEMP_ERROR)
        );
        if(!request.getPassword().equals(user.getPassword())){
            throw new ServiceException(ErrorCode.WRONG_PASSWORD);
        }

//        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
//            throw new ServiceException(ErrorCode.TEMP_ERROR);
//        }

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getUserRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

//        refresh token 저장 ... 로직
        RefreshToken tokenEntity = refreshTokenRepository.findById(user.getId())
                .orElse(new RefreshToken(user.getId(), refreshToken));

        refreshTokenRepository.save(tokenEntity);
        // 토큰 파싱 로직

        return LoginResponse.of(request, accessToken);
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
//        String customerUid = "CUST_" + UUID.randomUUID().toString().substring(0,8);
        // CUST_XXXXXXXX 의 customerUid 생성하기

        User user = userRepository.save(new User(
                request.getName(),
                null,
                request.getEmail(),
                request.getPassword(),
                request.getPhone()));

        return SignupResponse.of(user);
    }

    public GetMyInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                ()-> new ServiceException(ErrorCode.USER_NOT_FOUND)
        );

        return GetMyInfoResponse.of(user);

    }

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
