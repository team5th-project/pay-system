package com.bootcamp.paymentdemo.product;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.OrderItem;
import com.bootcamp.paymentdemo.order.service.OrderService;
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
        Product product = getProductById(Long.parseLong(orderItem.getProductId()));

        if (orderItem.getQuantity() >= product.getStock()){
            throw new ServiceException(ErrorCode.STOCK_NOT_ENOUGH);
        }
    }

    // orderItemList 에 주문 가능한 상품(재고 충분)만 담겨있는지 확인하는 메서드
    public boolean isOrderItemListValid(List<OrderItem> orderItemList){
        for (OrderItem orderItem : orderItemList) {
            isOrderItemEnough(orderItem);
        }
        return true;
    }



    // 소영 추가
    @Transactional
    public void decreaseStockByOrder(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            // TODO : // 현재 order 엔티티쪽에서 productId가 String으로 되어 있습니다. 이부분은 Long으로 바꾸는게 좋아보입니다.
            Product product = productRepository.findByIdForUpdate(Long.valueOf(orderItem.getProductId()));
            product.decreaseStock(orderItem.getQuantity());
        }
    }
}