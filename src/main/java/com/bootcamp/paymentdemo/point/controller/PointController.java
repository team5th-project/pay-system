package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.common.global.PageResponse;
import com.bootcamp.paymentdemo.point.dto.MembershipPolicyResponse;
import com.bootcamp.paymentdemo.point.dto.MyPointResponse;
import com.bootcamp.paymentdemo.point.dto.PointTransactionItem;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    private final UserPointService userPointService;

    // 현재 포인트 + 등급 조회
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MyPointResponse>> getMyPoint(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        MyPointResponse response = userPointService.getMyPoints(userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    // 포인트 거래내역 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<PointTransactionItem>>> getPointHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Long userId = userDetails.getUserId();
        // 요청값 검증 - size 상한선 방어 처리
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 50),
                pageable.getSort()
        );
        PageResponse<PointTransactionItem> response =
                PageResponse.from(userPointService.getPointHistory(userId, safePageable));
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }

    // 등급 정책 조회
    @GetMapping("/grades")
    public ResponseEntity<CommonResponse<List<MembershipPolicyResponse>>> getMembershipPolicies() {
        List<MembershipPolicyResponse> response = userPointService.getMembershipPolicies();
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}