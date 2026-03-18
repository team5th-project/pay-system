package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.product.dto.GetProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    //1. 상품 다건 조회
    @GetMapping
    ResponseEntity<CommonResponse<List<GetProductResponse>>> getProducts(){
        List<GetProductResponse> responses = productService.getProducts();
        return CommonResponseHandler.success(HttpStatus.OK, responses);
//        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    //2. 상품 단건 조회
    @GetMapping("/{productId}")
    ResponseEntity<CommonResponse<GetProductResponse>> getOne(@PathVariable Long productId){
        GetProductResponse response = productService.getOne(productId);
//        return ResponseEntity.status(HttpStatus.OK).body(response);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}
