package com.bootcamp.paymentdemo.order.controller;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.order.dto.request.OrderCreateRequest;
import com.bootcamp.paymentdemo.order.dto.response.OrderConfirmResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderCreateResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderDetailResponse;
import com.bootcamp.paymentdemo.order.dto.response.OrderListResponse;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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

    //  내 주문 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<List<OrderListResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<OrderListResponse> response = orderService.getMyOrders(userId);
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