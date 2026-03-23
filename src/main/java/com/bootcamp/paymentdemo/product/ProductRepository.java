package com.bootcamp.paymentdemo.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
            "(:productCategory IS NULL OR p.productCategory = :productCategory) ")
    Page<Product> searchProducts(ProductCategory productCategory, Pageable pageable);

    // 소영 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Product findByIdForUpdate(@Param("productId") Long productId);
}
