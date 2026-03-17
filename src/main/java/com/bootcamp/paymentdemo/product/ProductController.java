package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.product.dto.GetProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    //1. 상품 다건 조회
    @GetMapping("/api/products")
    ResponseEntity<List<GetProductResponse>> getProducts(){
        List<GetProductResponse> responses = productService.getProducts();
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    //2. 상품 단건 조회
    @GetMapping("/api/products/{productId}")
    ResponseEntity<GetProductResponse> getOne(@PathVariable Long productId){
        GetProductResponse response = productService.getOne(productId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
