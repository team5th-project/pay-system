package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.point.entity.*;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.product.ProductService;
import com.bootcamp.paymentdemo.refund.dto.request.CreateRefundRequest;
import com.bootcamp.paymentdemo.refund.dto.response.CreateRefundResponse;
import com.bootcamp.paymentdemo.refund.dto.response.GetRefundDetailResponse;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancellationDto;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.PortOneRefundStatus;
import com.bootcamp.paymentdemo.refund.enums.RefundFailureCode;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import com.bootcamp.paymentdemo.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static aQute.bnd.annotation.headers.Category.payment;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentService paymentService;
    private final PortOneRefundService portOneRefundService;
    private final UserPointService userPointService;
    private final ProductService productService;

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

            processRefundCallback(refund.getId(), status);
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

    public Refund getRefundByPaymentUid(String paymentUid) {
        return refundRepository.findByPaymentPaymentUid(paymentUid).orElseThrow(
                () -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND)
        );
    }

    @Transactional
    public void handleRefundSucceeded(Refund refund, Payment payment, Order order) {
        // 중복처리 방지
        if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
            return;
        }
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 후속 처리
        userPointService.refundPoint(order.getUserId(), order.getId(), refund.getPayment().getFinalAmount());
        productService.restoreStockByOrder(order);
        // 상태전이
        refund.complete();
        payment.refund();
        order.refund();
    }

    public void processRefundCallback(Long refundId, PortOneRefundStatus status) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));
        Payment payment = refund.getPayment();
        Order order = payment.getOrder();

        switch (status) {
            case SUCCEEDED -> handleRefundSucceeded(refund, payment, order);
            case REQUESTED -> { // 요청 상태는 일단 유지
            }
            case FAILED, UNKNOWN -> {
                refund.fail();
                throw new ServiceException(ErrorCode.REFUND_FAILED);
            }
        }
    }

    public void retryFailedRefund(Long id) {

        // 재시도 대상 환불 조회
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));
        // 재시도 가능한지 체크(상태, 실패코드, 횟수)
        if (!refund.canRetry(3)) {
            return;
        }
        // 포트원 호출직전 REQUESTED 상태변환
        refund.markRetryRequested();

        // 포트원 호출에 사용할 결제 정보 가져오기
        Payment payment = refund.getPayment();

        try { // 포트원 환불 API 다시 호출
            PortOneCancellationDto cancellationDto =
                    portOneRefundService.cancelPayment(payment.getPaymentUid(), refund.getReason());

            // 포트원 응답에서 상태값 가져옴
            PortOneRefundStatus status = cancellationDto.status();
            // 기존 성공처리 로직 사용
            processRefundCallback(refund.getId(), status);

        } catch (ServiceException e) {
            // 우리가 의도적으로 던진 예외(비즈니스나 포트원 에러 매핑)
            RefundFailureCode failureCode = mapFailureCode(e.getErrorCode()); // Errorcode 기반으로 failureCode로 변환
            // 실패 상태 + 실패코드 + 메시지 저장
            refund.markFailed(failureCode, e.getMessage());
        } catch (Exception e) {
            // 그 외의 모든 예외
            refund.markFailed(RefundFailureCode.PORTONE_COMMUNICATION_ERROR, e.getMessage());
        }
    }

    // 공통에러 코드를 환불 도메인에서 사용하는 실패 코드로 변환
    private RefundFailureCode mapFailureCode(ErrorCode errorCode) {
        return switch (errorCode) {
            case PORTONE_SERVER_ERROR -> RefundFailureCode.PORTONE_SERVER_ERROR;
            case PORTONE_COMMUNICATION_ERROR -> RefundFailureCode.PORTONE_COMMUNICATION_ERROR;
            case PORTONE_UNKNOWN_ERROR -> RefundFailureCode.PORTONE_UNKNOWN_ERROR;
            case INVALID_PAYMENT_STATUS -> RefundFailureCode.INVALID_PAYMENT_STATUS;
            case INVALID_REFUND_STATUS -> RefundFailureCode.INVALID_REFUND_STATUS;
            case PORTONE_PAYMENT_NOT_FOUND -> RefundFailureCode.PORTONE_PAYMENT_NOT_FOUND;
            case UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST -> RefundFailureCode.UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST;
            default -> RefundFailureCode.PORTONE_UNKNOWN_ERROR;
        };
    }
    }
