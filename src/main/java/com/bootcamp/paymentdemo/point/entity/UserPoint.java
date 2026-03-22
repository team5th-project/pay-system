package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class UserPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private int totalPoint; //원래 보유하고 있는 포인트
    private int usedPoint; // 사용이 확정된 포인트
    private int heldPoint; // 가점유

    public int getAvailablePoint() {
        return totalPoint - usedPoint - heldPoint;
    }

    public void hold(int amount) {
        if (amount <= 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        if (getAvailablePoint() < amount) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.heldPoint += amount;
    }

    // 결제가 완료되지 않아 가점유를 해제
    public void release(int amount) {
        if (this.heldPoint < amount){
            throw new ServiceException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.heldPoint -= amount;
    }

    // 결제가 일어나서 가점유 되었던 포인트를 실제로 차감
    public void commit(int amount) {
        this.heldPoint -= amount;
        this.usedPoint += amount;
    }

    // 포인트 차감
    // 결제했다가 환불한 경우 total 금액 차감해줘야함
    public void deductPoint(int amount) {
        this.totalPoint -= amount;
        this.usedPoint -= amount;
    }

    public void expire(int amount){
        if (getAvailablePoint() < amount) {
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



}