package com.bootcamp.paymentdemo.security.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BlacklistRepository extends JpaRepository<BlacklistToken, Long> {

    void deleteByExpirationTimeBefore(LocalDateTime now);

    boolean existsByToken(String token);
}
