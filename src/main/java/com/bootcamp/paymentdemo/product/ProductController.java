package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.common.global.PageResponse;
import com.bootcamp.paymentdemo.product.dto.GetProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    //1. 상품 다건 조회
    @GetMapping
    ResponseEntity<CommonResponse<PageResponse<GetProductResponse>>> getProducts(
            @RequestParam(required = false) ProductCategory productCategory,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ){
        PageResponse<GetProductResponse> responses = PageResponse.from(productService.getProducts(productCategory, pageable));
        return CommonResponseHandler.success(HttpStatus.OK, responses);
    }

    //2. 상품 단건 조회
    @GetMapping("/{productId}")
    ResponseEntity<CommonResponse<GetProductResponse>> getOne(@PathVariable Long productId){
        GetProductResponse response = productService.getOne(productId);
//        return ResponseEntity.status(HttpStatus.OK).body(response);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}
