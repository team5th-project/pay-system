package com.bootcamp.paymentdemo.order.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int quantity;

    //정적 팩토리 메서드

    public static OrderItem create(Order order, String productId,
                                   String productName, int price, int quantity) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
    }
}
