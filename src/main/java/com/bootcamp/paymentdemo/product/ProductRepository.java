package com.bootcamp.paymentdemo.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE " +
            "(:productCategory IS NULL OR p.productCategory = :productCategory) ")
    Page<Product> searchProducts(ProductCategory productCategory, Pageable pageable);
}
