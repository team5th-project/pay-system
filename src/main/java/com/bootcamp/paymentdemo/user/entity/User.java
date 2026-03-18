package com.bootcamp.paymentdemo.user.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.point.entity.MembershipGrade;
import com.bootcamp.paymentdemo.user.dto.SignupRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "users")
@Entity
@NoArgsConstructor
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
    private Long pointBalance;

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
        this.pointBalance = 0L;
        this.totalOrderAmount = 0L;
        this.membershipGrade = MembershipGrade.NORMAL;
    }
}
