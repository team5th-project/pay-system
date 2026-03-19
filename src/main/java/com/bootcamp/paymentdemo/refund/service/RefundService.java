package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.refund.dto.request.CreateRefundRequest;
import com.bootcamp.paymentdemo.refund.dto.response.CreateRefundResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundDetailResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundListResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    private void validateRefundable(Payment payment, Order order) {
        // 결제 상태 검증
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 주문 상태 검증
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    // service를 거쳐서 repository를 사용하는 방식으로 해주세요
    @Transactional
    public CreateRefundResponse requestRefund(Long paymentId, CreateRefundRequest request, Long userId) {
        // 결제 조회(paymentService를 통해 호출)
        Payment payment = paymentService.getPaymentById(paymentId);

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

        // TODO portone 호출
        try {
//            cancelPaymentToPortOne(payment); // mock 성공 처리
            // 성공 처리 상태 변경
            refund.complete(); // 환불 완료시 상태 전이
            payment.refund(); // 결제상태
            order.refund(); // 주문상태

            // TODO 이벤트 발행
//            eventPublisher.publishEvent(
//                    new RefundCompletedEvent(
//                            refund.getId(),
//                            payment.getId(),
//                            order.getId(),
//                            order.getCustomer().getId(),
//                            payment.getAmount()
//                    )
//            );
        }
        // 실패 처리
        catch (Exception e) {
            refund.fail();
            throw new ServiceException(ErrorCode.REFUND_FAILED);
        }
        return CreateRefundResponse.from(refund);
    }
    // TODO: PortOne API 호출
    //}    private void cancelPaymentToPortOne(Payment payment) {
    // mock 처리
//    log.info("PortOne 환불 요청 (mock): {}", payment.getPaymentUid());

    // 환불내역 목록 조회
    public GetRefundListResponse getRefunds(Long userId) {
        // 환불목록이 존재하는가?


        // TODO 환불목록 로직 추가
        return null;
    }

    public GetRefundDetailResponse getRefundDetail(Long refundId, Long userId) {

        // TODO 환불상세 조회 로직 추가
        return null;
    }
}

