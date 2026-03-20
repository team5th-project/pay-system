package com.bootcamp.paymentdemo.order.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.common.global.PageResponse;
import com.bootcamp.paymentdemo.order.dto.request.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.response.OrderConfirmResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderDetailResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderListResponse;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.entity.OrderItem;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.repository.OrderItemRepository;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        Long usedPoint = request.getUsedPoint() != null
                ? request.getUsedPoint() : 0L;


        // 2. 주문 생성
        Order order = Order.create(userId, totalAmount, usedPoint);
        orderRepository.save(order);

        // 3. 주문 상품 생성
        request.getItems().forEach(orderItemRequest -> {

            // TODO: 상품팀 API 연동 후 아래처럼 사용 예정
//            Long productIdLong = Long.parseLong(orderItemRequest.getProductId());

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


//
//      내 주문 목록 페이징 + 상태 필터 조회
//
//      - status가 null이면 전체 조회, 값이 있으면 해당 상태만 필터링
//      - Pageable을 파라미터로 받아 Controller에서 지정한 정렬·페이지 정보를 그대로 Repository에 전달
//      - Page<Order> → Page<OrderListResponse> 변환 후 PageResponse로 래핑
//        (PageResponse.from()은 content, page, size, totalElements, totalPages를 담아 반환)
//
//      @param userId   조회할 유저 ID
//      @param status   필터링할 주문 상태 (null 이면 전체 조회)
//      @param pageable 페이지 번호·사이즈·정렬 정보
//      @return 페이징 메타 정보 + 주문 목록
//
    public PageResponse<OrderListResponse> getMyOrders(Long userId, OrderStatus status, Pageable pageable) {
        // 1. userId + status 조건으로 필터링, 동적 정렬·페이징 적용
        Page<OrderListResponse> page = orderRepository
                .findByUserIdWithPaging(userId, status, pageable)
                // 2. Page<Order> → Page<OrderListResponse> 변환 (Spring Data map() 활용)
                .map(OrderListResponse::from);

        // 3. PageResponse로 래핑하여 반환 (content + 페이징 메타)
        return PageResponse.from(page);
    }

    // 주문 단건 조회
    public OrderDetailResponse getOrderDetail(Long userId, String orderUid) {
        Order order = orderRepository.findByOrderUid(orderUid)
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
    public OrderConfirmResponse confirmOrder(Long userId, String orderUid) {

        Order order = orderRepository.findByOrderUid(orderUid)
                .orElseThrow(() ->
                        new ServiceException(ErrorCode.ORDER_NOT_FOUND));

        // 본인 주문인지 확인
        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(ErrorCode.ORDER_NOT_OWNED);

        }

        order.confirm(); //상태 전이 (PAID에서 CONFIRMED)

        // 추후 포인트팀 적립 트리거 (EDD 이벤트 발행하면 상호작 용 예정)
        return OrderConfirmResponse.from(order);
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