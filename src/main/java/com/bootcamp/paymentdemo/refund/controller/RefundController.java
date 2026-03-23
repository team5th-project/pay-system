package com.bootcamp.paymentdemo.refund.controller;

import com.bootcamp.paymentdemo.common.global.CommonResponse;
import com.bootcamp.paymentdemo.common.global.CommonResponseHandler;
import com.bootcamp.paymentdemo.refund.dto.request.CreateRefundRequest;
import com.bootcamp.paymentdemo.refund.dto.response.CreateRefundResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundDetailResponse;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import com.bootcamp.paymentdemo.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;
    // 환불 요청
    @PostMapping("/{paymentUid}")
    public ResponseEntity<CommonResponse<CreateRefundResponse>> requestRefund(
            @PathVariable String paymentUid,
            @Valid @RequestBody CreateRefundRequest refundRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CreateRefundResponse response = refundService.requestRefund(
                paymentUid, refundRequest, userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.CREATED, response);
    }
    // 환불 상세조회
    @GetMapping("/{refundId}")
    public ResponseEntity<CommonResponse<GetRefundDetailResponse>> getRefundDetail(
            @PathVariable Long refundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        GetRefundDetailResponse response = refundService.getRefundDetail(refundId, userDetails.getUserId());
        return CommonResponseHandler.success(HttpStatus.OK, response);
    }
}
