package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.product.dto.GetProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    // 1. 상품 목록 조회
    public Page<GetProductResponse> getProducts(ProductCategory productCategory, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(productCategory, pageable);

        return products
                .map(GetProductResponse::of);
    }

    //2. 상품 단건 조회
    public GetProductResponse getOne(Long productId) {
        Product product = getProductById(productId);

        return GetProductResponse.of(product);
    }

    // 3. getProduct 메서드
    public Product getProductById(Long productId){
        return productRepository.findById(productId).orElseThrow(
                ()-> new ServiceException(ErrorCode.PRODUCT_NOT_FOUND)
        );
    }
}