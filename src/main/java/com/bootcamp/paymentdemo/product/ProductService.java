package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.product.dto.GetProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    public List<GetProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
                .map(GetProductResponse::of)
                .toList();
    }

    public GetProductResponse getOne(Long productId) {
        Product product = getProductById(productId);

        return GetProductResponse.of(product);
    }

    public Product getProductById(Long productId){
        return productRepository.findById(productId).orElseThrow(
                ()-> new IllegalArgumentException("존재하지 않는 상품입니다.")
        );
    }
}