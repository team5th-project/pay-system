package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.UserPoint;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select p from UserPoint p where p.userId = :userId")
//    UserPoint findByUserIdForUpdate(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPoint p where p.user.id = :userId")
    UserPoint findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select p from UserPoint p where p.user.id = :userId")
    UserPoint findByUserId(@Param("userId") Long userId);
}
