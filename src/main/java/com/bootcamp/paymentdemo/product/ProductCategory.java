package com.bootcamp.paymentdemo.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    GROCERY("사료, 간식"),
    TOY("장난감"),
    TOILET("화장실 용품")
    ;

    private final String name;
}
