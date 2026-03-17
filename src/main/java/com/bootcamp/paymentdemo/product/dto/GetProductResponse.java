package com.bootcamp.paymentdemo.product.dto;

import com.bootcamp.paymentdemo.product.Product;

public record GetProductResponse(
        String id,
        String name,
        int price,
        int stock
){
    public static GetProductResponse of(Product product){
        return new GetProductResponse(
                product.getId().toString(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
    }
}

