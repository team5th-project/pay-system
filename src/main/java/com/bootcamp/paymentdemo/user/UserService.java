package com.bootcamp.paymentdemo.user;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.point.entity.UserPoint;
import com.bootcamp.paymentdemo.point.repository.UserPointRepository;
import com.bootcamp.paymentdemo.security.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.token.BlacklistRepository;
import com.bootcamp.paymentdemo.security.token.BlacklistToken;
import com.bootcamp.paymentdemo.security.token.RefreshToken;
import com.bootcamp.paymentdemo.security.token.RefreshTokenRepository;
import com.bootcamp.paymentdemo.user.dto.*;
import com.bootcamp.paymentdemo.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlacklistRepository blacklistRepository;
    private final UserPointRepository userPointRepository;

    // 1. login
    @Transactional
    public InternalLoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new ServiceException(ErrorCode.USER_NOT_FOUND)
        );

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ServiceException(ErrorCode.WRONG_PASSWORD);
        }

        // access token 과 refresh 토큰을 각각 발급하고
        // refresh token 은 repository 에 저장, access token 은 header 로 반환
        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getUserRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // refresh token 은 로그인 할 때 마다 새로 생성됩니다
        // 만약에 repository 에 이미 사용자의 refresh token 이 존재한다면 갱신하고
        // repository 에 refresh token 이 없으면 새로 생성합니다.

        // repository 에 있는지 확인
        RefreshToken token = refreshTokenRepository.findByUserId(user.getId()).orElse(null);

        if (token != null) {
            token.updateToken(refreshToken); // 있으면 update (dirty checking)
        } else {
            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .userId(user.getId())
                            .token(refreshToken)
                            .build()
            ); // 없으면 새로 생성
        }

        return InternalLoginResponse.of(user, accessToken);
    }

    // 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {
//        String customerUid = "CUST_" + UUID.randomUUID().toString().substring(0,8);
        // CUST_XXXXXXXX 의 customerUid 생성하기
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(encodedPassword)
                .phone(request.getPhone())
                .build();


        UserPoint userPoint = UserPoint.builder().user(user).build();
        userRepository.save(user);
        userPointRepository.save(userPoint);

        return SignupResponse.of(user);
    }

    //logout
    @Transactional
    public void logout(String accessToken) {
        // 1 access 토큰 가져오기
        String token = accessToken.substring(7);
        Long userId = Long.valueOf(jwtTokenProvider.getId(token));

        // 2. 토큰에서 원래의 만료 시간 추출
        LocalDateTime expirationTime = jwtTokenProvider.getExpirationDateTime(token);

        // 블랙리스트 등록
        BlacklistToken blacklistToken = BlacklistToken.builder()
                .token(token)
                .expirationTime(expirationTime)
                .build();

        blacklistRepository.save(blacklistToken);
        refreshTokenRepository.deleteById(userId);
    }

    // 현재 로그인 한 사용자의 정보 출력
    public GetMyInfoResponse getMyInfo(Long userId) {
        User user = getUser(userId);
        UserPoint userPoint = userPointRepository.findByUserId(user.getId());
        int availablePoint = userPoint.getAvailablePoint();

        return GetMyInfoResponse.of(user, availablePoint);

    }

    //refresh token 재발급 로직
    @Transactional
    public String reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
        }

        Long userId = Long.valueOf(jwtTokenProvider.getId(refreshToken));
        User user = getUser(userId);

        RefreshToken savedToken = refreshTokenRepository.findById(userId).orElseThrow(
                () -> new ServiceException(ErrorCode.JWT_NOT_FOUND));

        if (!savedToken.getToken().equals(refreshToken)) {
            throw new ServiceException(ErrorCode.JWT_INVALID);
        }

        return jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getUserRole().toString());
    }

    // 내부 서비스용 User 엔티티 조회
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
    }
}
