package com.bootcamp.paymentdemo.product.dto;

import com.bootcamp.paymentdemo.product.Product;

public record GetProductResponse(
        String id,
        String name,
        Long price,  // int → Long으로 변경 (Product.price 타입 통일)
        int stock,
        String picture,
        String category
){
    public static GetProductResponse of(Product product){
        return new GetProductResponse(
                product.getId().toString(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getUrl(),
                product.getProductCategory().getName()
        );
    }
}

