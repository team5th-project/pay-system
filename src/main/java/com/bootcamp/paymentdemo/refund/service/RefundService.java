package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.refund.dto.request.CreateRefundRequest;
import com.bootcamp.paymentdemo.refund.dto.response.CreateRefundResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundDetailResponse;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancellationDto;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.PortOneRefundStatus;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final PortOneRefundService portOneRefundService;

    private void validateRefundable(Payment payment, Order order) {
        // 결제 상태 검증
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        // 민교가 수정함
        // 주문 상태 검증
        // - 기존: CONFIRMED 상태일 때만 환불 가능 → 주문 확정 이후에도 환불 가능해지는 잘못된 흐름
        // - 변경: PAID 상태일 때만 환불 가능
        //   → 결제 완료(PAID) 후 7일 이내에만 환불 가능, CONFIRMED 이후에는 환불 불가
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 환불 가능 시각 검증
        LocalDateTime paidAt = payment.getPaidAt(); // 결제성공 시각
        LocalDateTime expireAt = paidAt.plusDays(7); // 결제성공 시각 7일 이후
        LocalDateTime now = LocalDateTime.now(); // 환불진행하려는 현재시각

        // 현재 시각이 expireAt 이후 시간이라면 예외처리
        if (now.isAfter(expireAt)) {
            throw new ServiceException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }
    }

    @Transactional
    public CreateRefundResponse requestRefund(String paymentUid, CreateRefundRequest request, Long userId) {
        // 결제 조회(paymentService를 통해 호출)
        Payment payment = paymentService.getPaymentById(paymentUid);

        Order order = payment.getOrder();

        // userid랑 주문한 userId랑 같은지 검증
        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(ErrorCode.ORDER_NOT_OWNED);
        }

        // 멱등성 체크
        Refund refund = refundRepository.findByPaymentId(payment.getId()).orElse(null);
        if (refund != null) {
            RefundStatus status = refund.getRefundStatus();

            // 환불완료 및 환불요청 상태일 시 재시도 불가
            if (status == RefundStatus.COMPLETED || status == RefundStatus.REQUESTED) {
                return CreateRefundResponse.from(refund);
            }
        }
        // 주문, 결제 상태 검증 메서드
        validateRefundable(payment, order);
        // 환불이 빈값이면 새로 생성 환불실패일 경우 재시도 허용
        if (refund != null && refund.getRefundStatus() == RefundStatus.FAILED) {
            refund.retry(request.getReason());
        } else {
            // 환불 생성
            refund = Refund.create(payment, request.getReason());
            // 저장
            refundRepository.save(refund);
        }

        try {
            PortOneCancellationDto cancellationDto =
                    portOneRefundService.cancelPayment(payment.getPaymentUid(), request.getReason());
            PortOneRefundStatus status = cancellationDto.status();

            switch (status) {
                case SUCCEEDED -> {
                    refund.complete(); // 환불 완료시 상태 전이
                    payment.refund(); // 결제상태
                    order.refund(); // 주문상태
                }
                // TODO: 환불 완료 후 포인트 복구 / 재고 복구 / 멤버십 갱신 : RefundCompletedEvent 발행하여 리스너를 통해
                case REQUESTED -> {
                    // 요청 상태 유지
                }
                case FAILED, UNKNOWN -> {
                    refund.fail();
                    throw new ServiceException(ErrorCode.REFUND_FAILED);
                }
            }
        } catch (ServiceException e) {
            throw e;
        }
        // 실패 처리
        catch (Exception e) {
            refund.fail();
            throw new ServiceException(ErrorCode.REFUND_FAILED);
        }
        return CreateRefundResponse.from(refund);
    }

    public GetRefundDetailResponse getRefundDetail(Long refundId, Long userId) {
        // 저장된 환불 건과 동일한 환불 건인지 검증
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));

        Order order = refund.getPayment().getOrder();

        // 로그인한 사용자가 해당 환불 건의 주문 소유자인지 검증
        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(ErrorCode.ORDER_NOT_OWNED);
        }
        return GetRefundDetailResponse.from(refund);
    }
}
