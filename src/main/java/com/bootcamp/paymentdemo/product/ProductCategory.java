package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
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

    public static ProductCategory from(String categoryName){
        if (categoryName == null || categoryName.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_CATEGORY); // 카테고리 입력값이 null 또는 공백
        }

        for (ProductCategory p: ProductCategory.values()){
            if(ProductCategory.valueOf(categoryName) == p){
                return p;
            }
        }
        throw new ServiceException(ErrorCode.INVALID_CATEGORY); // 존재하지 않는 카테고리
    }
}
