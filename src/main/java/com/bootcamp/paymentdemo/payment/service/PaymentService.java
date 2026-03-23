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
import com.bootcamp.paymentdemo.payment.enums.PaymentResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
            // 재시도 포함한 포트원 결제 조회
            PortOnePaymentDto portOnePaymentDto = getPaymentWithRetry(paymentUid);
            // 조회 성공한 경우 결제 결과 가져오기
            PaymentResult result = getPaymentResult(portOnePaymentDto);

            return handlePaymentResult(payment, portOnePaymentDto, result);
        }
        /*
        PENDING인 경우 웹훅에서 결과가 PAID 일 때
        정상 성공 처리 가능하면 SUCCESS
        정상 성공 처리 불가능하면 CANCEL_REQUESTED -> 취소 -> REFUNDED
         */
        catch (InterruptedException e) { // 작업 중단, 결제 상태 미확정, PENDING 유지
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

    private boolean shouldTreat404AsPending(Payment payment) {
        // 결제 생성 후 1분 동안 오는 404 에러는 pending 상태로 유지. PG -> 포트원 동기화 지연 가능성 때문
        return payment.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1));
    }
    // 결제 결과 상태 분기 처리
    private ConfirmPaymentResponse handlePaymentResult(
            Payment payment,
            PortOnePaymentDto portOnePaymentDto,
            PaymentResult result
    ) {
        return switch (result) {
            case SUCCESS -> handleSuccess(payment, portOnePaymentDto);
            case FAIL -> handleFail(payment);
            case PENDING -> handlePending(payment);
        };
    }

    // SUCCESS 처리
    private ConfirmPaymentResponse handleSuccess(Payment payment, PortOnePaymentDto portOnePaymentDto) {
        if (!isPaidAmountMatched(payment, portOnePaymentDto)) {
            return handleAmountMismatch(payment);
        }

        try {// 결제 확정 성공
            markPaymentSuccess(payment);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.SUCCESS);
        } catch (ServiceException e) {// 결제는 성공했지만 내부 사정으로 취소해야 하는경우. 결제 확정 실패 처리
            requestCancelAfterInternalFailure(payment);
            throw e;
        }
    }
    // 결제는 되었는데 금액 불일치인 경우 ..
    private ConfirmPaymentResponse handleAmountMismatch(Payment payment) {
        // 그냥 취소 처리보다는, 결제된 건에 대하여 취소 요청 후 취소 요청으로 상태 변경
        requestCancelAfterInternalFailure(payment);
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.CANCEL_REQUESTED);
    }

    // 결제 성공했는데 재고 문제로 결제 취소 요청 보내야 하는 경우
    private void requestCancelAfterInternalFailure(Payment payment) {
        markCancelRequested(payment.getId());
        try {
            portOneService.cancelPayment(payment.getPaymentUid());
        } catch (RuntimeException cancelEx) {
            // 여기서는 일단 CANCEL_REQUESTED 유지
            log.warn("Payment Cancel Requested Failed - {}", cancelEx.getMessage());
            // 로그 남기고 재시도 대상 처리
            return;
        }
        markCancelled(payment.getId());
    }

    // FAIL 처리
    private ConfirmPaymentResponse handleFail(Payment payment) {
        markPaymentFailed(payment);
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.FAILED);
    }

    // PENDING 처리
    private ConfirmPaymentResponse handlePending(Payment payment) {
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.PENDING);
    }

    /*
    결제 취소 요청 상태로 변경
    현재 진행 중인 트랜잭션을 잠시 멈추고, 완전히 새로운 트랜잭션을 시작하는 옵션.
    재고 취소 요청에 실패하더라도 취소 요청했다는 기록은 롤백되면 안되기 때문. 취소 처리도 마찬가지
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelRequested(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.cancelRequested();
    }

    /*
    결제 취소 요청 성공으로 상태 변경
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelled(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.cancelled();
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

    // 결제 확정 실패 상태 전이
    private void markPaymentFailed(Payment payment){
        Order order = payment.getOrder();

        // 결제 상태 실패로 변경
        payment.failed();

        // 주문 상태는 PENDING으로 유지. 호출할 것 없음

        // TODO : 포인트 락 해제는 웹훅이 왔을 때 ..할까? 고민중..
        if (payment.getPointToUse() > 0) {
            userPointService.cancelUsePoint(order.getUserId(), order.getId(), payment.getPointToUse());
        }

    }

    // 결제 금액 일치 검증
    private boolean isPaidAmountMatched(Payment payment, PortOnePaymentDto paymentDto) {
        return payment.getFinalAmount().equals(paymentDto.amount());
    }

    // 포트원 조회 (재시도 포함 3회)
    private PortOnePaymentDto getPaymentWithRetry(String paymentUid) throws InterruptedException {

        int retry = 0;

        while (retry < 3) { // 조회 총 3번 시도
            try {
                PortOnePaymentDto payment = portOneService.getPayment(paymentUid);
                if (payment.status() == PortOnePaymentStatus.PAID) {
                    return payment;
                }
                if (payment.status() == PortOnePaymentStatus.READY
                        || payment.status() == PortOnePaymentStatus.PAY_PENDING) {
                    retry++;
                    Thread.sleep(1000);
                    continue;
                }
                return payment;
            } catch (RuntimeException e) {
                if(isRetryable(e)){ // 네트워크 에러인 경우 재시도
                    retry ++;
                    Thread.sleep(1000); // ⭐ 추가
                }else{  // 재시도 할 수 없는 PortOneException 에러인 경우 그대로 던지기
                    throw e;
                }
            }
        }
        // 네트워크 에러로 조회 재시도에도 불구하고 조회 실패 시 ServiceException 던지기
        throw new ServiceException(ErrorCode.PORTONE_UNAVAILABLE);
    }

    // 재시도가 가능한 에러인지, 그냥 실패처리 해야 할 에러인지 판단하는 메서드
    private boolean isRetryable(RuntimeException e) {
        return !(e instanceof PortOneException);
    }

    // 포트원 조회 성공 시 상태 검증
    private PaymentResult getPaymentResult(PortOnePaymentDto portOnePaymentDto) {
        PortOnePaymentStatus status = portOnePaymentDto.status();
        return switch (status) {
            case PAID -> PaymentResult.SUCCESS;
            case READY, PAY_PENDING -> PaymentResult.PENDING;
            default -> PaymentResult.FAIL;
        };
    }

    private String createPaymentId() {
        return "PAY-" + UUID.randomUUID();
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        }
    }
