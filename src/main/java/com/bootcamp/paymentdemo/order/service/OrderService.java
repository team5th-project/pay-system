package com.bootcamp.paymentdemo.order.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.dto.request.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.response.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderDetailResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderListResponse;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.entity.OrderItem;
import com.bootcamp.paymentdemo.order.repository.OrderItemRepository;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // 주문 생성
    @Transactional
    public OrderCreateResponse createOrder(Long userId,
                                           OrderCreateRequest request) {
        // 1. 총 금액 계산 (product와 협의 후 수정예정)
        Long totalAmount = request.getItems().stream()
                .mapToLong(item -> item.getQuantity())
                .sum();   // product팀 api 연동 후 실제 가격으로 변경

        // 2. 주문 생성
        Order order = Order.create(userId, totalAmount);
        orderRepository.save(order);

        // 3. 주문 상품 생성
        request.getItems().forEach(orderItemRequest -> {
            OrderItem orderItem = OrderItem.create(
                    order,
                    orderItemRequest.getProductId(),
                    "상품명", // product팀 api 연동후 변경
                    0L,                  // product팀 api 연동후 변경
                    orderItemRequest.getQuantity()
            );
            orderItemRepository.save(orderItem);
        });
        return OrderCreateResponse.from(order);
    }


    // 내 주문 목록 조회
    public List<OrderListResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderListResponse::from)
                .toList();
    }

    // 주문 단건 조회
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUserId().equals(userId)){
            throw new ServiceException(ErrorCode.ORDER_NOT_OWNED);
        }
        return OrderDetailResponse.from(order);
    }

    // 주문 확정
    @Transactional
    public void confirmOrder(Long userId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(ErrorCode.ORDER_NOT_OWNED);

        }

        order.confirm(); //상태 전이 (PAID에서 CONFIRMED)

        // 추후 포인트팀 적립 트리거 (EDD 이벤트 발행하면 상호작 용 예정)
    }

    // 소영 추가. Payment에서 사용하는 orderUid로 order 객체 검색하는 메서드
    public Order getOrderByOrderUid(String orderUid) {
        if (orderUid == null || orderUid.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_UID);
        }

        return orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));
    }
}