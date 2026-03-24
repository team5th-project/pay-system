package com.bootcamp.paymentdemo.common.config;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.MembershipPolicy;
import com.bootcamp.paymentdemo.point.repository.MembershipPolicyRepository;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.user.UserService;

import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 시작 시 필수 초기 데이터 삽입
 * 데이터가 이미 존재하면 삽입하지 않음 (중복 방지)
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final UserPointService userPointService;
    private final MembershipPolicyRepository membershipPolicyRepository;

    @Override
    public void run(ApplicationArguments args) {
        // 프론트 로그인 페이지에 표시된 테스트 계정
        String name = "권지원";
        String email = "admin@test.com";
        String password = "admin";


        userService.signup(SignupRequest.builder()
                        .name(name)
                        .email(email)
                        .phone("01012341234")
                        .password(password)
                .build());

        // 사용할 수 있는 포인트를 시작할 때 지급하는 로직을 추가했습니다.
        userPointService.grantPoint(1L, 50000);


        // MembershipPolicy 초기 데이터 삽입
        // 없으면 등급 갱신, 포인트 적립, 등급 정책 조회 API 동작 안 함
        // NORMAL(1%), VIP(5%), VVIP(10%)
        if (membershipPolicyRepository.count() == 0) {
            membershipPolicyRepository.saveAll(List.of(
                    MembershipPolicy.create(MembershipGrade.NORMAL, 0L, 50000L, 1),
                    MembershipPolicy.create(MembershipGrade.VIP, 50001L, 100000L, 5),
                    MembershipPolicy.create(MembershipGrade.VVIP, 100001L, null, 10)
            ));
        }
    }
}