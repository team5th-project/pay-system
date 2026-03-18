package com.bootcamp.paymentdemo.user;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
    private int pointBalance;

    public User(String name, String customerUid, String email, String password, String phone) {
        this.name = name;
        this.customerUid = customerUid;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.userRole = UserRole.USER;
        this.pointBalance = 0;
    }
}
