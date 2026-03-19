package com.bootcamp.paymentdemo.user.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Min(0)
    private int pointBalance;

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
        this.pointBalance = 0;
        this.totalOrderAmount = 0L;
        this.membershipGrade = MembershipGrade.NORMAL;
    }

    // User 엔티티에 추가
    public void deductPoint(int points) {
        if (this.pointBalance < points) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.pointBalance -= points;
    }

    public void addPoint(int points) {
        this.pointBalance += points;
    }

    public void updateGrade(MembershipGrade grade) {
        this.membershipGrade = grade;
    }


}
