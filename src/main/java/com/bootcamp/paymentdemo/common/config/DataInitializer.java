package com.bootcamp.paymentdemo.common.config;

import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.MembershipPolicy;
import com.bootcamp.paymentdemo.point.repository.MembershipPolicyRepository;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.user.UserRepository;
import com.bootcamp.paymentdemo.user.UserService;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import com.bootcamp.paymentdemo.user.dto.SignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final MembershipPolicyRepository membershipPolicyRepository;

    @Override
    @Transactional // 유저 생성 + 포인트 지급 로직을 하나의 트랜잭션으로 관리
    public void run(ApplicationArguments args) {
        // 1. 테스트용 계정 생성
        createTestUser("admin", "admin@test.com", "01012341234", "admin", 500000);
        createTestUser("볼드모트", "voldemort@test.com", "01042741234", "voldemort", 500000);
        createTestUser("간달프", "gandalf@test.com", "01080870903", "gandalf", 500000);
        createTestUser("도비", "dobby@test.com", "01006708024", "dobby", 500000);

        // 2. MembershipPolicy 초기 데이터 삽입
        // 데이터가 없을 때만 실행하여 중복 방지
        if (membershipPolicyRepository.count() == 0) {
            membershipPolicyRepository.saveAll(List.of(
                    MembershipPolicy.create(MembershipGrade.NORMAL, 0L, 49999L, 1),
                    MembershipPolicy.create(MembershipGrade.VIP, 50000L, 99999L, 5),
                    MembershipPolicy.create(MembershipGrade.VVIP, 100000L, null, 10)
            ));
        }
    }

    /**
     * 유저 중복 여부를 확인하고, 새 유저 생성 시 포인트까지 지급하는 헬퍼 메서드
     */
    private void createTestUser(String name, String email, String phone, String password, int pointAmount) {
        // UserRepository를 사용하여 해당 이메일의 유저가 있는지 확인
        if (userRepository.findByEmail(email).isEmpty()) {

            // 회원가입 후 생성된 유저 정보(ID 포함)를 결과로 받아옴
            SignupResponse response = userService.signup(SignupRequest.builder()
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .password(password)
                    .build());

            // 반환받은 실제 DB ID(response.id())를 사용하여 정확하게 포인트 지급
            userPointService.grantPoint(response.id(), pointAmount);
        }
    }
}