package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.refund.dto.request.CreateRefundRequest;
import com.bootcamp.paymentdemo.refund.dto.response.CreateRefundResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundDetailResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundListResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
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
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CreateRefundResponse requestRefund(Long paymentId, CreateRefundRequest request, Long userId) {

        // 결제 조회
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // 멱등성 체크
        // 환불 DB에서 찾은 결제환불내역과 환불내역과 같는지 체크
        Refund existingRefund = refundRepository.findByPaymentId(payment.getId()).orElse(null);
        if (existingRefund != null) { // null값이 아니라면 기존에 있는 환불 응답
            return CreateRefundResponse.from(existingRefund);
        }

        // 결제 상태 검증
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 주문 상태 검증
        Order order = payment.getOrder();
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // refund 생성
        Refund refund = Refund.create(payment, request.getReason());

        // 저장
        refundRepository.save(refund);

        // TODO portone 호출
        try {
//            cancelPaymentToPortOne(payment); // mock 성공 처리
            // 성공 처리 상태 변경
            refund.complete(); // 환불 완료시 상태전이
            payment.refund(); // 결제상태 전이
            order.refund(); // 주문상태 전이ㅁ

            // TODO 이벤트 발행
//            eventPublisher.publishEvent(
//                    new RefundCompletedEvent(
//                            refund.getId(),
//                            payment.getId(),
//                            order.getId(),
//                            order.getCustomer().getId(),
//                            payment.getAmount() // 이벤트 퍼블리셔가 이벤트를 퍼블리시를 하면 이벤트리스너가 캐치해서 사전에 정의된 작업들을 진행하게된다.
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

