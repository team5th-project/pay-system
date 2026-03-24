package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="user_points")
public class UserPoint extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int totalPoint; //원래 보유하고 있는 포인트
    @Column(nullable = false)
    private int usedPoint; // 사용이 확정된 포인트
    @Column(nullable = false)
    private int heldPoint; // 가점유

    @Builder
    public UserPoint(User user){
        this.user = user;
        this.totalPoint = 0;
        this.usedPoint = 0;
        this.heldPoint = 0;

    }

    public int getAvailablePoint() {
        return totalPoint - usedPoint - heldPoint;
    }

    // 결제 시 포인트 가점유
    public void hold(int amount) {
        if (amount <=0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        if (getAvailablePoint() < amount) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.heldPoint += amount;
    }

    // 결제가 완료되지 않아 가점유를 해제
    public void release(int amount) {
        if (amount <= 0 || this.heldPoint < amount) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        this.heldPoint -= amount;
    }

    // 결제가 일어나서 가점유 되었던 포인트를 실제로 차감
    public void commit(int amount) {
        if (amount <= 0 || this.heldPoint < amount) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        this.heldPoint -= amount;
        this.usedPoint += amount;
    }

    // 포인트 소멸(만료)
    public void expire(int amount) {
        if (amount <= 0 || getAvailablePoint() < amount) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        this.totalPoint -= amount;
    }


    // 포인트 적립
    // 주문 확정시 멤버십 등급에 따라 포인트 적립
    public void addPoint(int amount) {
        if (amount <= 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        this.totalPoint += amount;
    }

    // 포인트 복구
    // 환불 시 사용한 확정된 포인트를 다시 사용 가능 상태로
    // usedPoint만 줄이면 됨.
    // 예시: totalPoint=1000, usedPoint=500 -> 환불 후 totalPoint=1000, usedPoint=0
    public void restoreUsedPoint(int amount) {
        if (amount <= 0 || this.usedPoint < amount) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        this.usedPoint -= amount;
    }
}