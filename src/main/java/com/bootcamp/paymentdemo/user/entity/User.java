package com.bootcamp.paymentdemo.user.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.point.entity.UserPoint;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Table(name = "users")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerUid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Column(nullable = false)
    private Long totalOrderAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipGrade membershipGrade;

    @Builder
    public User(String name, String email, String password, String phone) {
        this.name = name;
        this.customerUid = null;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.userRole = UserRole.USER;
        this.totalOrderAmount = 0L;
        this.membershipGrade = MembershipGrade.NORMAL;
    }

    // 멤버십 등급 변경
// 결제 완료 및 환불 시 totalOrderAmount 기준으로 등급 재계산 후 호출
    public void updateGrade(MembershipGrade grade) {
        this.membershipGrade = grade;
    }

    // 누적 주문금액 증가
// 결제 완료 시 호출, 멤버십 등급 산정 기준값 사용
//    public void addTotalOrderAmount(long paymentAmount) {
//        if (paymentAmount <= 0) {
//            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
//        }
//        this.totalOrderAmount += paymentAmount;
//    }

    // 누적 주문금액 차감
// 환불 완료 시 호출, 차감 후 0 미만 되는 경우 0으로 초기화
//    public void deductTotalOrderAmount(long refundAmount) {
//        if (refundAmount <= 0) {
//            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
//        }
//        if (this.totalOrderAmount < refundAmount) {
//            this.totalOrderAmount = 0L;
//            return;
//        }
//        this.totalOrderAmount -= refundAmount;
//    }

    // total 주문 확정 금액
    // 주문 확정시 더해짐
    public void addTotalOrderAmount(int amount) {
        if (amount < 0){
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        this.totalOrderAmount += amount;
    }

}
