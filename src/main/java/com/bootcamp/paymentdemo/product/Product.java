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
    private Long price;  // int → Long으로 변경 (OrderItem.price 타입과 통일, 형변환 불필요)

    @Column(nullable = false)
    @Min(0)
    private int stock;

    private String url; // 상품 사진

    @Enumerated(value = EnumType.STRING)
    private ProductCategory productCategory;

    public Product(String name, Long price, int stock, String url, ProductCategory productCategory) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.url = url;
        this.productCategory = productCategory;
    }
}
