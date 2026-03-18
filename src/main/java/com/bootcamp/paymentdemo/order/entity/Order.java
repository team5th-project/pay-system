package com.bootcamp.paymentdemo.order.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public static Order create(Long userId, int totalAmount) {
        return Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.PENDING)
                .build();
    }

    private static String generateOrderNumber() {
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
        return "ORD-" + date + "-" + uuid;
    }

    public void markAsPaid() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태에서만 결제 완료로 변경할 수 있습니다.");
        }
        this.status = OrderStatus.PAID;
    }

    public void confirm() {
        if (this.status != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료 상태에서만 확정할 수 있습니다.");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void markAsFailed() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태에서만 실패 처리할 수 있습니다.");
        }
        this.status = OrderStatus.FAILED;
    }

    public void refund() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("주문 확정 상태에서만 환불할 수 있습니다.");
        }
        this.status = OrderStatus.REFUNDED;
    }
}
