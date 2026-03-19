package com.bootcamp.paymentdemo.order.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long totalAmount;  // 원래 주문 총액 (포인트 차감 전)

    @Column(nullable = false)
    private Long usedPoint;    // 사용한 포인트

    @Column(nullable = false)
    private Long finalAmount;  // 실제 결제 금액 (totalAmount - usedPoint)

    @Column(nullable = false, unique = true)
    private String orderUid;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(Long userId, Long totalAmount,
                  String orderUid, String orderNumber, OrderStatus status) {
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.orderUid = orderUid;
        this.orderNumber = orderNumber;
        this.status = status;
    }


    public static Order create(Long userId, Long totalAmount, Long usedPoint) {
        Long finalAmount = totalAmount - usedPoint; // 최종금액 계싼
        return Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .orderUid(generateOrderUid())
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .build();
    }

    private static String generateOrderUid() {
        String uuid = UUID.randomUUID()
                .toString()
                .replace("-", "");  // 32자리
        return "ORD-" + uuid;
        // 결과: "ORD-a1b2c3d4e5f122334243243324"
    }
    // 6. TSID 생성 메서드 (임시)
    private static String generateOrderNumber() {
        // TODO: TSID 라이브러리 추가 후 변경 예정
        String temp = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
        return "ORD-" + temp;
        // 결과: "ORD-A1B2C3D4E5"
    }
    public void markAsPaid() {
        if (this.status != OrderStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.PAID;
    }

    public void confirm() {
        if (this.status != OrderStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void markAsFailed() {
        if (this.status != OrderStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.FAILED;
    }

    public void refund() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.REFUNDED;
    }
}
