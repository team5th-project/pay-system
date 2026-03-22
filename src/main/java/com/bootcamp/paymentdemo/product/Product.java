package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
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

    @Enumerated(value = EnumType.STRING)
    private ProductCategory productCategory;

    public Product(String name, int price, int stock, String url, ProductCategory productCategory) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.url = url;
        this.productCategory = productCategory;
    }




    // 소영 : 결제 성공 시 재고 차감 확인 및 주문 상태변경 확인을 위해 추가
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new ServiceException(ErrorCode.INVALID_PRODUCT_QUANTITY);
        }
        if (this.stock < quantity) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_STOCK);
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new ServiceException(ErrorCode.INVALID_PRODUCT_QUANTITY);
        }
        this.stock += quantity;
    }
}
