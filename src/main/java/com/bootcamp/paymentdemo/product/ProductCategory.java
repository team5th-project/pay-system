package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {
    MAGIC_WAND("지팡이"),
    CRYSTAL_BALL("수정구"),
    BOOK("전공책"),
    BROOM_PLAN("마법 빗자루 플랜(구독)"),
    MEDICINE("물약"),
    POCKET_CAT("버프 고양이")
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
