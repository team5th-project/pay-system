package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Transactional(readOnly = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentService paymentService;
    private final PortOneRefundService portOneRefundService;
    private final UserPointService userPointService;
    private final ProductService productService;

    /**
     * 환불 요청 처리 시 기존 환불 존재 여부에 따라
     * 새 환불 생성 여부를 결정한다.
     */
    @Transactional
    public CreateRefundResponse requestRefund(String paymentUid, CreateRefundRequest request, Long userId) {
        // 결제 조회(paymentService를 통해 호출)
        Payment payment = paymentService.getPaymentByUid(paymentUid);

        Order order = payment.getOrder();

        // userid랑 주문한 userId랑 같은지 검증
        if (!order.getUserId().equals(userId)) {
            throw new ServiceException(ErrorCode.ORDER_NOT_OWNED);
        }

        // 멱등성 체크
        Refund refund = refundRepository.findByPaymentId(payment.getId()).orElse(null);
        if (refund != null) {
            RefundStatus status = refund.getRefundStatus();
            /**
             * 이미 환불이 생성된 경우에는 "새 환불"을 다시 만들지 않고 기존 환불 상태를 그대로 반환한다.
             *
             * - COMPLETED:
             *   이미 환불이 최종 완료된 상태이므로 새 요청을 만들 필요가 없음
             *
             * - REQUESTED:
             *   환불 엔티티는 생성되었고 아직 외부 처리 전/초기 요청 상태이므로 기존 건 반환
             *
             * - PROCESSING:
             *   포트원 외부 환불 요청이 이미 진행 중인 상태이므로 새 요청을 만들면 안 됨
             *
             * - FAILED_RETRYABLE:
             *   자동 재시도 대상 상태이므로 requestRefund() 에서 다시 새 환불을 만들지 않고
             *   스케줄러/재처리 로직이 기존 환불 건을 처리하도록 둔다
             *
             * - FAILED_FINAL:
             *   자동 재시도는 종료된 상태이므로 새 환불을 다시 생성하지 않고 기존 상태를 반환한다
             *
             * 즉, 기존 refund가 있으면 이 메서드는 "새 환불 생성"이 아니라
             * "기존 환불 상태 조회/응답" 역할만 수행한다.
             */
            if (status == RefundStatus.COMPLETED
                    || status == RefundStatus.REQUESTED
                    || status == RefundStatus.PROCESSING
                    || status == RefundStatus.FAILED_FINAL
                    || status == RefundStatus.FAILED_RETRYABLE) {
                return CreateRefundResponse.from(refund);
            }
        }
        // 주문, 결제 상태 검증
        validateRefundable(payment, order);
        // 환불 생성
        refund = Refund.create(payment, request.getReason());
        refundRepository.save(refund);

        // 포인트 전액 결제와 일반 결제 분기
        // - 포인트 전액 결제:
        //   포트원 외부 취소가 필요 없으므로 내부 포인트 복구/재고 복구/상태 변경만 처리

        // - 일반 결제 or 포인트 일부 사용 결제:
        //   실제 결제금액이 존재하므로 포트원 외부 환불 요청 필요
        if (isPointOnlyPayment(payment)) {
            handlePointOnlyRefund(refund, payment, order);
            return CreateRefundResponse.from(refund);
        }
        // 포트원 외부 환불 요청 직전 PROCESSING 상태로 전환
        refund.markProcessing();

        // 포트원 외부 환불 요청 시작
        try {
            PortOneCancellationDto cancellationDto =
                    portOneRefundService.cancelPayment(payment.getPaymentUid(), request.getReason());

            // 포트원 응답 상태 기반 후처리
            PortOneRefundStatus status = cancellationDto.status();
            processRefundCallback(refund.getId(), status);
        } catch (ServiceException e) {
            // 포트원/비즈니스 예외는 ErrorCode를 RefundFailureCode로 변환 후
            RefundFailureCode failureCode = mapFailureCode(e.getErrorCode());
            // retryable / final 여부 체크 후 환불 상태 반영
            handleRetryFailure(refund, failureCode, e.getMessage());
            throw e;

        } catch (Exception e) {
            // 예상치 못한 예외는 일단 통신 오류로 판단해 retryable failure 처리
            handleRetryFailure(refund, RefundFailureCode.PORTONE_COMMUNICATION_ERROR, e.getMessage());
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

    private void validateRefundable(Payment payment, Order order) {
        // 결제 상태 검증
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS_FOR_REFUND);
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
        if (paidAt == null) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        LocalDateTime expireAt = paidAt.plusDays(7); // 결제성공 시각 7일 이후
        LocalDateTime now = LocalDateTime.now(); // 환불진행하려는 현재시각

        // 현재 시각이 expireAt 이후 시간이라면 예외처리
        if (now.isAfter(expireAt)) {
            throw new ServiceException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }
    }

    @Transactional
    public void handleRefundSucceeded(Refund refund, Payment payment, Order order) {
        // 중복처리 방지
        if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
            return;
        }
        // 상태 체크
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 후속 처리
        if (order.getUsedPoint() > 0) {
            userPointService.refundPoint(order.getUserId(), order.getId(), order.getUsedPoint());
        }
        productService.restoreStockByOrder(order);
        // 상태전이
        refund.markCompleted();
        payment.refund();
        order.refund();
        // 로그
        if (payment.getPointToUse() > 0) {
            log.info("포인트 부분 결제 환불 성공 userId={}, orderId={}, refundId={}, restoredPoint={}, refundedAmount={}",
                    order.getUserId(),
                    order.getId(),
                    refund.getId(),
                    payment.getPointToUse(),
                    payment.getFinalAmount());
        } else {
            log.info("일반 결제 환불 성공 userId={}, orderId={}, refundId={}, refundedAmount={}",
                    order.getUserId(),
                    order.getId(),
                    refund.getId(),
                    payment.getFinalAmount());
        }
    }

    // 결제금액이 0원이 맞는지, 실제 사용포인트가 있는지 체크
    private boolean isPointOnlyPayment(Payment payment) {
        return payment.getFinalAmount() == 0 && payment.getPointToUse() > 0;
    }

    // 포인트 전액 결제 환불
    @Transactional
    public void handlePointOnlyRefund(Refund refund, Payment payment, Order order) {
        log.info("포인트 전액 결제 환불 시작 paymentUid={}, finalAmount={}, pointToUse={}, orderFinalAmount={}",
                payment.getPaymentUid(),
                payment.getFinalAmount(),
                payment.getPointToUse(),
                order.getFinalAmount());

        // 이미 환불 완료된 경우 중복 처리 방지
        if (refund.getRefundStatus() == RefundStatus.COMPLETED ||
                payment.getPaymentStatus() == PaymentStatus.REFUNDED ||
                order.getStatus() == OrderStatus.REFUNDED) {
            return;
        }
        // 포인트 복구
        userPointService.refundPoint(order.getUserId(), order.getId(), payment.getPointToUse());
        // 재고 복구
        productService.restoreStockByOrder(order);
        // 상태전이
        refund.markCompleted();
        payment.refund();
        order.refund();
        log.info("포인트 전액 결제 환불 성공 userId={}, orderId={}, refundId={}, restoredPoint={}",
                order.getUserId(),
                order.getId(),
                refund.getId(),
                payment.getPointToUse());
    }

    //
    public void processRefundCallback(Long refundId, PortOneRefundStatus status) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));
        Payment payment = refund.getPayment();
        Order order = payment.getOrder();

        switch (status) {
            case SUCCEEDED -> handleRefundSucceeded(refund, payment, order);
            case REQUESTED -> {
                // 아직 포트원 처리진행중이면 PROCESSING 유지
                refund.markProcessing();
            }
            case FAILED -> {
                handleRetryFailure(
                        refund,
                        RefundFailureCode.PORTONE_UNKNOWN_ERROR,
                        "포트원 환불 상태 FAILED"
                );
            }
            case UNKNOWN -> {
                handleRetryFailure(
                        refund,
                        RefundFailureCode.PORTONE_UNKNOWN_ERROR,
                        "포트원 환불 상태 UNKNOWN"
                );
            }
        }
    }

    @Transactional
    public void retryRefund(Long id) {
        Refund refund = refundRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ServiceException(ErrorCode.REFUND_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        //  상태 검증
        if (refund.getRefundStatus() != RefundStatus.FAILED_RETRYABLE) {
            log.info("재시도 상태가 아닙니다. refundId={}, status={}", refund.getId(), refund.getRefundStatus());
            return;
        }
        if (!refund.isRetryDue(now)) {
            log.info("아직 재시도 시간이 아닙니다. refundId={}, nextRetryAt={}", refund.getId(), refund.getNextRetryAt());
            return;
        }

        Payment payment = refund.getPayment();

        log.info("환불 재시도 시작 refundId={}, paymentUid={}, retryCount={}",
                refund.getId(), payment.getPaymentUid(), refund.getRetryCount());

        // 재시도 시작 (PROCESSING)
        refund.markProcessing();

        try {
            PortOneCancellationDto cancellationDto =
                    portOneRefundService.cancelPayment(payment.getPaymentUid(), refund.getReason());

            PortOneRefundStatus status = cancellationDto.status();

            Order order = payment.getOrder();

            switch (status) {
                case SUCCEEDED -> {
                    handleRefundSucceeded(refund, payment, order);
                    log.info("환불 재시도 성공 refundId={}, paymentUid={}", refund.getId(), payment.getPaymentUid());
                }
                case REQUESTED -> {
                    // 포트원에는 재요청이 들어갔고 아직 완료 안 된 상태
                    // 다시 FAILED_RETRYABLE로 내리지 말고 PROCESSING 유지
                    refund.markProcessing();
                    log.info("환불 재시도 후 아직 처리중 refundId={}, paymentUid={}",
                            refund.getId(), payment.getPaymentUid());
                }
                case FAILED, UNKNOWN -> {
                    handleRetryFailure(refund, RefundFailureCode.PORTONE_UNKNOWN_ERROR, "포트원 환불 재시도 결과 미확정");
                }
            }

        } catch (ServiceException e) {
            RefundFailureCode failureCode = mapFailureCode(e.getErrorCode());
            handleRetryFailure(refund, failureCode, e.getMessage());
        } catch (Exception e) {
            // generic 예외도 일단 retryable 쪽으로 보내는 게 더 자연스럽다
            handleRetryFailure(refund, RefundFailureCode.PORTONE_COMMUNICATION_ERROR, e.getMessage());
        }
    }

    // 재시도 중 실패 처리
    private void handleRetryFailure(Refund refund, RefundFailureCode failureCode, String message) {
        int currentRetryCount = refund.getRetryCount();
        LocalDateTime now = LocalDateTime.now();

        // 1. 애초에 재시도 불가한 실패면 바로 최종 실패
        if (!failureCode.isRetryable()) {
            refund.markFinalFailure(failureCode, message);
            logRetryFailure(refund, "환불 재시도 최종 실패(재시도 불가)", true);
            return;
        }

        // 2. 재시도 가능한 실패지만 자동 재시도 한도를 넘기면 최종 실패(혹은 수동 확인 상태)
        if (RefundRetryPolicy.isAutoRetryExhausted(currentRetryCount)) {
            refund.markFinalFailure(failureCode, message);
            logRetryFailure(refund, "환불 재시도 최종 실패(재시도 한도 초과)", true);
            return;
        }

        // 3. 재시도 가능한 실패이고 아직 한도 안 넘었으면 다음 재시도 시간 계산
        LocalDateTime nextRetryAt = RefundRetryPolicy.calculateNextRetryAt(currentRetryCount, now);
        refund.markRetryableFailure(failureCode, message, nextRetryAt);
        logRetryFailure(refund, "환불 재시도 실패(재시도 예정)", false);
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

    // 실패기록 로그 메서드
    private void logRetryFailure(Refund refund, String logMessage, boolean isFinal) {
        if (isFinal) {
            log.error(
                    "{} refundId={}, paymentUid={}, retryCount={}, failureCode={}, failureReason={}, nextRetryAt={}, refundStatus={}",
                    logMessage,
                    refund.getId(),
                    refund.getPayment().getPaymentUid(),
                    refund.getRetryCount(),
                    refund.getFailureCode(),
                    refund.getFailureReason(),
                    refund.getNextRetryAt(),
                    refund.getRefundStatus()
            );
        } else {
            log.warn(
                    "{} refundId={}, paymentUid={}, retryCount={}, failureCode={}, failureReason={}, nextRetryAt={}, refundStatus={}",
                    logMessage,
                    refund.getId(),
                    refund.getPayment().getPaymentUid(),
                    refund.getRetryCount(),
                    refund.getFailureCode(),
                    refund.getFailureReason(),
                    refund.getNextRetryAt(),
                    refund.getRefundStatus()
            );
        }
    }
}
