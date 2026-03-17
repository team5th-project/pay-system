package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "products")
@Entity
@NoArgsConstructor
@Getter
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Min(0)
    private int price;

    @Column(nullable = false)
    @Min(0)
    private int stock;

    private String url; // 상품 사진

    public Product(String name, int price, int stock, String url) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.url = url;
    }
}
