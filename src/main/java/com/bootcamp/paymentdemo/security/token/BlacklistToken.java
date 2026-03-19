package com.bootcamp.paymentdemo.security.token;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="logout_access_tokens")
@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class BlacklistToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    // 저장되고 일정 시간 있다가 삭제되어야하니까 삭제될 시간 적어두기
    private LocalDateTime expirationTime;

}
