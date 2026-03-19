package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.point.dto.MembershipPolicyResponse;
import com.bootcamp.paymentdemo.point.dto.MyPointResponse;
import com.bootcamp.paymentdemo.point.dto.PointHistoryResponse;
import com.bootcamp.paymentdemo.point.service.PointService;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    // 현재 포인트 + 등급 조회
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MyPointResponse>> getMyPoint(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        MyPointResponse response = pointService.getMyPoints(userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    // 포인트 거래내역 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<PointHistoryResponse>> getPointHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PointHistoryResponse response = pointService.getPointHistory(userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    // 등급 정책 조회
    @GetMapping("/grades")
    public ResponseEntity<CommonResponse<List<MembershipPolicyResponse>>> getMembershipPolicies() {
        List<MembershipPolicyResponse> response = pointService.getMembershipPolicies();
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}