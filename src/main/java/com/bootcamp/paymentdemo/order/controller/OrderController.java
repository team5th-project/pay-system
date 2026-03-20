package com.bootcamp.paymentdemo.order.controller;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.common.global.PageResponse;
import com.bootcamp.paymentdemo.order.dto.request.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.response.OrderConfirmResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderDetailResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderListResponse;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    //  주문 생성
    @PostMapping
    public ResponseEntity<CommonResponse<OrderCreateResponse>> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody OrderCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        OrderCreateResponse response = orderService.createOrder(userId, request);
        return CommonResponseHandler.success(HttpStatus.CREATED, response);
    }

//
//      내 주문 목록 페이징 + 상태 필터 조회
//
//      - @RequestParam status: 주문 상태 필터 (선택값)
//        → 없으면 전체 조회, 있으면 해당 상태만 필터링
//        → 예: ?status=PAID, ?status=CONFIRMED
//      - @PageableDefault: 클라이언트가 아무 파라미터도 안 보내면 적용되는 기본값
//        → size=10 (한 페이지 10건), createdAt DESC (최신순)
//      - 동적 정렬: ?sort=totalAmount,asc 처럼 보내면 그 기준으로 덮어씀
//
//      요청 예시)
//        GET /api/orders                          → 전체 조회 (10건, 최신순)
//        GET /api/orders?status=PAID              → 결제완료 주문만
//        GET /api/orders?status=CONFIRMED         → 확정 주문만
//        GET /api/orders?page=0&size=5            → 5건씩 페이징
//       GET /api/orders?sort=totalAmount,desc    → 금액 높은 순 정렬
//
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<OrderListResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            // 주문 상태 필터 (선택값) - null이면 전체 조회
            @RequestParam(required = false) OrderStatus status,
            // 기본값: size=10, createdAt 기준 최신순 / ?sort= 파라미터로 동적 변경 가능
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long userId = userDetails.getUserId();
        PageResponse<OrderListResponse> response = orderService.getMyOrders(userId, status, pageable);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
    //  주문 단건 조회
    @GetMapping("/{orderUid}")
    public ResponseEntity<CommonResponse<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderUid
    ) {
        Long userId = userDetails.getUserId();
        OrderDetailResponse response = orderService.getOrderDetail(userId, orderUid);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    //  주문 확정
    @PatchMapping("/{orderUid}/confirm")
    public ResponseEntity<CommonResponse<OrderConfirmResponse>> confirmOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String orderUid
    ) {
        Long userId = userDetails.getUserId();
        OrderConfirmResponse response = orderService.confirmOrder(userId, orderUid);
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}