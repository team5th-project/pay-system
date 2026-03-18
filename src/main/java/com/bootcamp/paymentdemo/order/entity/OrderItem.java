package com.bootcamp.paymentdemo.order.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private Long price;

    @Column(nullable = false)
    private int quantity;


    @Builder
    private OrderItem(Order order, String productId,
                      String productName, Long price, int quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }
    //정적 팩토리 메서드
    public static OrderItem create(Order order, String productId,
                                   String productName, Long price, int quantity) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
    }
}
