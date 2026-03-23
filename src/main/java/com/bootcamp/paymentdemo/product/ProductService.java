package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.OrderItem;
import com.bootcamp.paymentdemo.order.entity.Order;
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

    public int getProductStockById(Long productId){
        return getProductById(productId).getStock();
    }

    public void isOrderItemEnough(OrderItem orderItem){
        // getProductId() → getProduct().getId() 변경 (#101 - OrderItem Product 연관관계 변경)
        Product product = getProductById(orderItem.getProduct().getId());

        if (orderItem.getQuantity() >= product.getStock()){
            throw new ServiceException(ErrorCode.STOCK_NOT_ENOUGH);
        }
    }

    // orderItemList 에 주문 가능한 상품(재고 충분)만 담겨있는지 확인하는 메서드
    public boolean isOrderItemListValid(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            isOrderItemEnough(orderItem);
        }
        return true;
    }

    // 소영 추가
    @Transactional
    public void decreaseStockByOrder(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            // getProductId() -> getProduct().getId() 변경 (#101 - OrderItem Product 연관관계 변경)
            // String -> Long 변환 불필요 (Product 객체에서 직접 ID 조회)
            Product product = productRepository.findByIdForUpdate(orderItem.getProduct().getId());
            product.decreaseStock(orderItem.getQuantity());
        }
    }
}