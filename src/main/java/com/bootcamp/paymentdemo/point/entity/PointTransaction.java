package com.bootcamp.paymentdemo.point.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column
    private Long orderId; // nullable - 소멸은 주문 없음

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type;

    private LocalDateTime expiredAt; // 적립 시에만 존재

    @Builder
    private PointTransaction(Long userId, Long orderId, int points,
                             PointType type, LocalDateTime expiredAt) {
        this.userId = userId;
        this.orderId = orderId;
        this.points = points;
        this.type = type;
        this.expiredAt = expiredAt;
    }

    // 포인트 적립
    public static PointTransaction earn(Long userId, Long orderId,
                                        int points, LocalDateTime expiredAt) {
        if (points <= 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        return PointTransaction.builder()
                .userId(userId)
                .orderId(orderId)
                .points(points)
                .type(PointType.EARN)
                .expiredAt(expiredAt)
                .build();
    }

    // 포인트 사용
    public static PointTransaction use(Long userId, Long orderId, int points) {
        if (points <= 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        return PointTransaction.builder()
                .userId(userId)
                .orderId(orderId)
                .points(-points) // 차감이니까 음수
                .type(PointType.USE)
                .build();
    }

    // 포인트 소멸
    public static PointTransaction expire(Long userId, int points) {
        if (points <= 0) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        return PointTransaction.builder()
                .userId(userId)
                .points(-points)
                .type(PointType.EXPIRE)
                .build();
    }
}