package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.PortOneException;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.dto.request.CreatePaymentRequest;
import com.bootcamp.paymentdemo.payment.dto.response.ConfirmPaymentResponse;
import com.bootcamp.paymentdemo.payment.dto.response.CreatePaymentResponse;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentCheckResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PortOneService portOneService;
    private final UserPointService userPointService;
    private final ProductService productService;
    private final PaymentVerificationService paymentVerificationService;
    private final PaymentStatusTxService paymentStatusTxService;
    @Transactional
    public CreatePaymentResponse createPayment(String orderUid, CreatePaymentRequest request) {
        Order order = orderService.getOrderByOrderUid(orderUid);

        // request 데이터 추출
        Long totalAmount = request.getTotalAmount();
        int pointToUse = request.getPointToUse() == null ? 0 : request.getPointToUse();


        // 주문 상태 검증
        if(order.getStatus()!= OrderStatus.PENDING){
            throw new ServiceException(ErrorCode.ORDER_STATUS_NOT_PENDING);
        }

        // 중복 결제 방지
        // 같은 주문에 대해서 결제 요청 후 대기중(PENDING) 상태인 결제가 있는 경우 결제 생성 불가능
        boolean existence = paymentRepository.existsByOrderAndPaymentStatus(order, PaymentStatus.PENDING);
        if(existence){
            throw new ServiceException(ErrorCode.ALREADY_PENDING_PAYMENT);
        }

        // 주문 금액 null 검증
        if (totalAmount == null) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 결제 요청시 들어온 금액과 주문 금액이 일치하는지 검증
        if (!totalAmount.equals(order.getTotalAmount())) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        // 사용하려는 포인트가 있을때에만 포인트 락 걸기
        if (pointToUse > 0) {

            // 사용 포인트량이 주문 금액보다 작거나 같은지 검증
            if (pointToUse > totalAmount) {
                throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
            }

            // 포인트 가점유. 포인트 쪽에서 포인트 사용 가능 여부 확인
            // userId, orderId, point
            userPointService.holdPoint(order.getUserId(), order.getId(), pointToUse);
        }

        // 실제 결제 금액 계산
        Long finalAmount = totalAmount - pointToUse;

        // 결제 생성
        Payment payment = Payment.builder()
                .paymentUid(createPaymentId())
                .order(order)
                .finalAmount(finalAmount)
                .pointToUse(pointToUse)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        return CreatePaymentResponse.from(payment);
    }

    // 결제 확정 요청
    @Transactional
    public ConfirmPaymentResponse confirmPayment(String paymentUid) {

        // paymentId 검증
        if (!StringUtils.hasLength(paymentUid)) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_UID);
        }
        // DB에서 Payment 객체 조회. 없으면 생성되지 않은 결제 요청
        Payment payment = paymentRepository.findByPaymentUid(paymentUid).orElseThrow(
                () -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // Payment 상태 검증. 결제 대기 상태가 아니면 이미 결제 완료 처리 되었거나 환불되었거나 결제 취소처리 된 결제 요청으로 판단
        if (!payment.getPaymentStatus().equals(PaymentStatus.PENDING)) {
            throw new ServiceException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        // Order 상태 검증. 근데 이거 필요한가?? 결제 생성할때는 상태가 pending 이었다가 바뀌면 어떡함? 결제는 실제로 되었을 수도 있는거 아닌가?
        Order order = payment.getOrder();
        if(!OrderStatus.PENDING.equals(order.getStatus())){
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 포트원 조회
        try {
            PaymentCheckResult result = paymentVerificationService.checkPayment(payment.getPaymentUid());
            return handlePaymentResult(payment, result.portOnePaymentDto(), result.paymentResult());
        } catch (InterruptedException e) { // 작업 중단, 결제 상태 미확정, PENDING 유지
            Thread.currentThread().interrupt();
            log.warn("PortOne confirm interrupted. paymentUid={}", paymentUid, e);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.PENDING);
        } catch (RestClientException e) {   // 결과 조회 불가, 결제 상태 미확정, PENDING 유지
            log.warn("PortOne network error. paymentUid={}", paymentUid, e);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.PENDING);

        } catch (PortOneException e) {
            if (e.getErrorCode() == ErrorCode.PORTONE_PAYMENT_NOT_FOUND
                    && shouldTreat404AsPending(payment)) {
                return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.PENDING);
            }
            markPaymentFailed(payment);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.FAILED);
        } catch (RuntimeException e) { // 상태 변경 없이 롤백, 서버 에러 응답,나중에 운영 로그 확인
            log.error("Unexpected confirmPayment error. paymentUid={}", paymentUid, e);
            throw e;
        }
    }

    // 결제 결과 상태 분기 처리
    private ConfirmPaymentResponse handlePaymentResult(Payment payment, PortOnePaymentDto portOnePaymentDto, PaymentResult result) {
        return switch (result) {
            case SUCCESS -> handleSuccess(payment, portOnePaymentDto);
            case FAIL -> handleFail(payment);
            case PENDING -> handlePending(payment);
        };
    }

    /*
    결제 성공시 메서드
     */
    // SUCCESS 처리
    private ConfirmPaymentResponse handleSuccess(Payment payment, PortOnePaymentDto portOnePaymentDto) {
        if (!isPaidAmountMatched(payment, portOnePaymentDto)) { // 결제 금액 불일치
            return handleAmountMismatch(payment);
        }

        try {// 결제 확정 성공
            markPaymentSuccess(payment);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.SUCCESS);
        } catch (ServiceException e) {// 결제는 성공했지만 내부 사정으로 취소해야 하는경우. 결제 확정 실패 처리
            PaymentStatus status = requestCancelAfterInternalFailure(payment);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), status);
        }
    }

    // 결제 확정 성공 상태 전이
    private void markPaymentSuccess(Payment payment){
        Order order = payment.getOrder();

        // 결제 상태 성공으로 변경
        payment.success();

        // 주문 상태 결제 성공으로 변경
        order.markAsPaid();

        // 재고 차감
        productService.decreaseStockByOrder(order);

        // 포인트 차감
        if (payment.getPointToUse() > 0) {
            userPointService.usePoint(order.getUserId(), order.getId(), payment.getPointToUse());
        }

    }

    /*
    결제 실패 시 메서드
     */

    // FAIL 처리
    private ConfirmPaymentResponse handleFail(Payment payment) {
        markPaymentFailed(payment);
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.FAILED);
    }

    // 결제 확정 실패 상태 전이
    private void markPaymentFailed(Payment payment){
        Order order = payment.getOrder();

        // 결제 상태 실패로 변경
        payment.failed();

        // 주문 상태는 PENDING으로 유지. 호출할 것 없음

        // 포인트 가점유 해제
        if (payment.getPointToUse() > 0) {
            userPointService.cancelUsePoint(order.getUserId(), order.getId(), payment.getPointToUse());
        }

    }

    /*
    결제 취소 시 메서드
     */
    // 결제는 되었는데 금액 불일치인 경우 ..
    private ConfirmPaymentResponse handleAmountMismatch(Payment payment) {
        // 그냥 취소 처리보다는, 결제된 건에 대하여 취소 요청 후 취소 요청으로 상태 변경
        PaymentStatus status = requestCancelAfterInternalFailure(payment);
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), status);
    }

    // 결제 성공했는데 재고 문제로 결제 취소 요청 보내야 하는 경우
    private PaymentStatus requestCancelAfterInternalFailure(Payment payment) {
        paymentStatusTxService.markCancelRequested(payment.getId());
        try {
            portOneService.cancelPayment(payment.getPaymentUid());
            paymentStatusTxService.markRefunded(payment.getId());
        } catch (RuntimeException e) {
            log.warn("Payment cancel failed. paymentUid={}", payment.getPaymentUid(), e);
            return PaymentStatus.CANCEL_REQUESTED;
        }
        paymentStatusTxService.markCancelled(payment.getId());
        return PaymentStatus.CANCELLED;
    }





    /*
    PENDING 시 메서드
     */

    // PENDING 처리
    private ConfirmPaymentResponse handlePending(Payment payment) {
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.PENDING);
    }

    private boolean shouldTreat404AsPending(Payment payment) {
        // 결제 생성 후 1분 동안 오는 404 에러는 pending 상태로 유지. PG -> 포트원 동기화 지연 가능성 때문
        return payment.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1));
    }

    // 결제 금액 일치 검증
    private boolean isPaidAmountMatched(Payment payment, PortOnePaymentDto paymentDto) {
            return payment.getFinalAmount().equals(paymentDto.amount());
    }

    private String createPaymentId() {
        return "PAY-" + UUID.randomUUID();
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        }
    }
