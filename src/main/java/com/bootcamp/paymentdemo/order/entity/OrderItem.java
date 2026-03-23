package com.bootcamp.paymentdemo.order.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.product.Product;
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

    // 어떤 주문에 속한 상품인지 → Order와 FK 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Product 엔티티와 @ManyToOne 연관관계로 변경 (#101)
     *
     * - 기존: String productId → Product 테이블과 FK 관계 없음, 타입 불일치 (Long PK를 String으로 저장)
     * - 변경: Product 객체 직접 참조 → FK 제약 조건 적용, 존재하지 않는 상품 저장 방지
     * - Order ↔ OrderItem 과 동일한 방식으로 연관관계 적용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 주문 시점의 상품명 스냅샷
    // → 추후 상품명이 변경되어도 주문 당시 상품명을 보존하기 위해 별도 컬럼으로 저장
    @Column(nullable = false)
    private String productName;

    // 주문 시점의 상품 가격 스냅샷
    // → 추후 상품 가격이 변경되어도 주문 당시 가격을 보존하기 위해 별도 컬럼으로 저장
    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private int quantity;

    @Builder
    private OrderItem(Order order, Product product,
                      String productName, Long price, int quantity) {
        this.order = order;
        this.product = product;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * OrderItem 정적 팩토리 메서드
     *
     * - String productId 파라미터 제거, Product 객체 직접 받도록 변경 (#101)
     * - productName, price는 주문 시점 스냅샷으로 별도 저장
     *   → 상품 정보가 변경되어도 주문 내역은 당시 정보 유지
     *
     * @param order       소속 주문
     * @param product     주문한 상품 엔티티
     * @param productName 주문 시점 상품명 스냅샷
     * @param price       주문 시점 가격 스냅샷
     * @param quantity    주문 수량
     */
    public static OrderItem create(Order order, Product product,
                                   String productName, Long price, int quantity) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
    }
}
