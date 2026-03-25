package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.dto.request.CreatePaymentRequest;
import com.bootcamp.paymentdemo.payment.dto.response.ConfirmPaymentResponse;
import com.bootcamp.paymentdemo.payment.dto.response.CreatePaymentResponse;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentCancelResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
//@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PortOneService portOneService;
    private final UserPointService userPointService;
    private final PaymentVerificationService paymentVerificationService;
    private final PaymentStatusTxService paymentStatusTxService;
    private final PaymentCancelService paymentCancelService;
    @Transactional
    public CreatePaymentResponse createPayment(String orderUid, CreatePaymentRequest request) {
        Order order = orderService.getOrderByOrderUid(orderUid);

        // request 데이터 추출
        Long totalAmount = request.getTotalAmount();
        int pointsToUse = request.getPointToUse() == null ? 0 : request.getPointToUse();

        // 주문 상태 검증
        if(order.getStatus()!= OrderStatus.PENDING){
            throw new ServiceException(ErrorCode.ORDER_STATUS_NOT_PENDING);
        }

        // 동일 주문에 대하여 중복 결제 생성 허용
        // 같은 주문에 대해서 결제 요청 후 대기중(PENDING) 상태인 결제가 있어도 결제 생성 가능
        // success 된 결제가 있으면 막기
        boolean existence = paymentRepository.existsByOrderAndPaymentStatus(order, PaymentStatus.SUCCESS);
        if(existence){
            throw new ServiceException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        // 주문 금액 null 검증
        if (totalAmount == null) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 결제 요청시 들어온 금액과 주문 금액이 일치하는지 검증
        if (!totalAmount.equals(order.getTotalAmount())) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        // 사용하려는 포인트량이 주문 금액보다 크면 에러
        if (pointsToUse > totalAmount) {
            throw new ServiceException(ErrorCode.INVALID_POINT_AMOUNT);
        }
        // 실 결제 금액이 0원 초과 1000원 미만이면 에러. 0원이면 전액 포인트 결제로 간주하고 결제 생성 가능
        long finalAmount = totalAmount - pointsToUse;
        if (0< finalAmount && finalAmount < 1000) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        // 결제 생성
        Payment payment = Payment.builder()
                .paymentUid(createPaymentId())
                .order(order)
                .finalAmount(finalAmount)
                .pointToUse(pointsToUse)
                .paymentStatus(PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))  // 스케쥴러에서 5분동안 결제가 안되면 failed 처리하기
                .build();

        // 사용하려는 포인트가 있을때에만 포인트 락 걸기
        if (pointsToUse > 0) {

            // 포인트 가점유. 포인트 쪽에서 포인트 사용 가능 여부 확인
            // userId, orderId, point
            userPointService.holdPoint(order.getUserId(), order.getId(), pointsToUse);
        }
        paymentRepository.save(payment);

        return CreatePaymentResponse.from(payment);
    }

    // 결제 확정 요청
    public ConfirmPaymentResponse confirmPayment(String paymentUid) {

        // paymentId 존재 여부 검증
        if (!StringUtils.hasLength(paymentUid)) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_UID);
        }
        // DB에서 Payment 객체 조회. 없으면 생성되지 않은 결제 요청
        Payment payment = paymentRepository.findByPaymentUid(paymentUid).orElseThrow(
                () -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // Payment 상태 검증. 결제 대기 상태가 아니면 이미 처리 된 결제 요청으로 판단. 어떤 상태인지 모르니 실제 상태 반환.
        if (PaymentStatus.PENDING != payment.getPaymentStatus()) {
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), payment.getPaymentStatus());
        }

        // Order 상태 검증.
        // 결제 생성할때는 주문 상태가 pending 이었다가 결제 확정 요청 시에는 이미 다른 결제 시도/스케쥴러/웹훅에서 처리되어서 주문 성공 상태일 수 있으니 검증
        Order order = payment.getOrder();
        if(OrderStatus.PENDING != order.getStatus()){
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 결제 금액과 포인트 사용 검증
        if (payment.getFinalAmount() == 0 ) {
            // 포인트로 전액 결제해서 실 결제 금액이 0원이라면 포트원 검증 안하고, DB에 재고 반영만 하고 결제 성공 처리
            paymentStatusTxService.markSuccess(payment.getPaymentUid());
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.SUCCESS);
        }

        // 실 결제 금액이 존재하면 포트원 조회
        PaymentResult result = paymentVerificationService.checkPayment(payment);
        return handlePaymentResult(payment, result);
    }

    // 결제 결과 상태 분기 처리
    private ConfirmPaymentResponse handlePaymentResult(Payment payment, PaymentResult paymentCheckResult) {

        return switch (paymentCheckResult) {
            case SUCCESS -> handleSuccess(payment);
            case FAIL -> handleFail(payment);
            case AMOUNT_MISMATCH -> handleAmountMismatch(payment);
            case CANCELLED -> new ConfirmPaymentResponse(payment.getOrder().getOrderUid(), PaymentStatus.CANCELLED);
        };
    }

    /*
    결제 성공시 메서드
     */
    // SUCCESS 처리
    private ConfirmPaymentResponse handleSuccess(Payment payment) {

        System.out.println("PaymentService.handleSuccess");

        try {// 결제 성공
            paymentStatusTxService.markSuccess(payment.getPaymentUid());
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.SUCCESS);
        } catch (ServiceException e) {
            System.out.println("e.getMessage() = " + e.getMessage());
            // 결제는 성공했지만 내부 사정으로 취소해야 하는경우. 결제 확정 실패 처리
            PaymentStatus status = requestCancelAfterInternalFailure(payment);
            return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), status);
        }
    }

//    // 결제 확정 성공 상태 전이
//    private void markPaymentSuccess(Payment payment){
//        Order order = payment.getOrder();
//
//        // 결제 상태 성공으로 변경
//        payment.success();
//
//        // 주문 상태 결제 성공으로 변경
//        order.markAsPaid();
//
//        // 재고 차감
//        productService.decreaseStockByOrder(order);
//
//        // 포인트 차감
//        if (payment.getPointToUse() > 0) {
//            userPointService.usePoint(order.getUserId(), order.getId(), payment.getPointToUse());
//        }
//
//    }


    // FAIL 처리
    private ConfirmPaymentResponse handleFail(Payment payment) {
        paymentStatusTxService.markFailed(payment.getPaymentUid());
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), PaymentStatus.FAILED);
    }

//    // 결제 확정 실패 상태 전이
//    private void markPaymentFailed(Payment payment){
//        Order order = payment.getOrder();
//        // 결제 상태 실패로 변경
//        payment.failed();
//        // 주문 상태는 PENDING으로 유지. 호출할 것 없음
//        // 포인트 가점유 해제
//        if (payment.getPointToUse() > 0) {
//            userPointService.cancelUsePoint(order.getUserId(), order.getId(), payment.getPointToUse());
//        }
//    }


    private ConfirmPaymentResponse handleAmountMismatch(Payment payment) {
        PaymentStatus status = requestCancelAfterInternalFailure(payment);
        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), status);
    }

    // 결제 성공했는데 내부 사정(결제 금액 불일치, 재고 부족 등) 결제 취소 요청 보내야 하는 경우
    private PaymentStatus requestCancelAfterInternalFailure(Payment payment) {
        System.out.println("PaymentService.requestCancelAfterInternalFailure");
        paymentStatusTxService.markCancelRequested(payment.getPaymentUid());
//        payment.cancelRequested();
        // 포트원 결제 취소 요청
        String reason = "Failed to process Payment Confirm in Server";
        PaymentCancelResult paymentCancelResult = paymentCancelService.processPaymentCancel(payment, reason);
        switch (paymentCancelResult) {
            case SUCCESS -> {
                paymentStatusTxService.markCancelled(payment.getPaymentUid());
                return PaymentStatus.CANCELLED;
            }
            case CANCEL_REQUESTED -> {
                return PaymentStatus.CANCEL_REQUESTED;
            }
            default -> {
                paymentStatusTxService.markCancelFailed(payment.getPaymentUid());
                return PaymentStatus.CANCEL_FAILED;
            }
        }
    }


    private String createPaymentId() {
        return "PAY-" + UUID.randomUUID();
    }

    public Payment getPaymentByUid(String paymentUid) {
        return paymentRepository.findByPaymentUid(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
    }

        /* 민교님 추가
           orderId로 결제 정보 조회
          - 주문 확정 시 포인트 적립을 위해 finalAmount(실제 PG 결제 금액) 가져올 때 사용
          - confirmOrder() 및 OrderScheduler에서 호출
          @param orderId 조회할 주문 ID
          @return 해당 주문의 Payment 객체
         */
        public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional
    public void completePaymentFromWebhook(Payment payment, PortOnePaymentDto portOnePaymentDto) {
        /*
        결제 확정 요청이 오지 않아서 PENDING 상태였던 결제 건에 대하여
        실제 결제가 성공했을 경우 검증 후 성공 처리
         */
        // 1. 이미 성공이면 멱등 처리
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        // 2. PENDING 상태인 결제가 맞는지 상태 검증
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 3. paymentUid 검증
        if (!payment.getPaymentUid().equals(portOnePaymentDto.paymentId())) {
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_NOT_EQUALS);
        }

        // 4. 금액 검증
        if (!payment.getFinalAmount().equals(portOnePaymentDto.amount())) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        paymentStatusTxService.markSuccess(payment.getPaymentUid());
    }

    /*
    결제 취소 요청
    1. 실제 결제가 되었는데 네트워크 오류로 payment는 실패 처리 되어있는 경우
    클라이언트가 결제 창을 누르고 결제를 성공했고, 서버에 결제 확정 요청이 들어와서 서버 -> 포트원으로 결제 조회를 했는데 네트워크 이슈로 결제 조회가 되지 않는 경우가 있습니다.
    이 때 재시도 포함 총 3회 조회 하는데도 결제 조회가 되지 않는 경우라서 성공 처리를 할 수가 없습니다.
    그런 경우 결제 상태를 failed 처리하고 클라이언트 측에 결과를 내려줍니다.
    이후에 웹훅이 도착했는데 사실은 결제 성공이 되었다고 하면 클라이언트 입장에서는 나 결제는 해서 돈 빠져나갔는데 왜 결제 실패라고 뜨지? 라고 의문이 들겁니다.
    이런 상황에서 웹훅에서 결제 성공이 되었다고 하면 이 돈을 다시 환불해주는 경우입니다.

    2. 서버 내부 오류로 결제 취소 요청을 보냈는데, 취소가 되지 않은 경우
    2번은 포트원 조회해서 결제 성공을 했는데 서버 내부 사정(재고 차감 하려고 보니 재고가 없음 / 결제 금액이 실제와 일치하지 않음) 으로 포트원으로 결제 취소 요청(CANCEL_REQUESTED)을 이미 보낸 상황입니다.
    그런데 그 취소 요청이 처리 된 것을 확인할 수 없는 경우 CANCEL_REQUESTED 상태가 유지되고, 만약 취소 요청이 실패 했다면 CANCEL_FAILED 상태가 됩니다.
    이는 추후 스케줄러에서도 취소 요청된 결제 건들을 조회해서 환불 처리가 잘 되었는지 확인하고 안되었다면 다시 환불 요청을 하는 로직이 추가될 예정이고요, 웹훅에서도 체크해서 취소 요청을 보내주는겁니다.
     */
    @Transactional
    public void requestCancelFromWebhook(Payment payment, PortOnePaymentDto portOnePaymentDto) {
            // 이미 취소된 경우 멱등 처리
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return;
        }
        // 2. FAILED,CANCEL_REQUESTED,CANCEL_FAILED 상태인 결제가 맞는지 상태 검증
        if (!(payment.getPaymentStatus() == PaymentStatus.FAILED
                || payment.getPaymentStatus() == PaymentStatus.CANCEL_REQUESTED
                || payment.getPaymentStatus() == PaymentStatus.CANCEL_FAILED)) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 3. paymentUid 검증
        if (!payment.getPaymentUid().equals(portOnePaymentDto.paymentId())) {
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_NOT_EQUALS);
        }
        // 4. 금액 검증
        if (!payment.getFinalAmount().equals(portOnePaymentDto.amount())) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        // 결제 취소 요청 메서드 호출 필요
        try {
            portOneService.cancelPayment(payment.getPaymentUid(), "Failed To Process Payment in Server");
            paymentStatusTxService.markCancelRequested(payment.getPaymentUid());

        } catch (RuntimeException e) {
            log.warn("Webhook 검증 후 결제 취소 요청 실패 - paymentId :{} ",payment.getPaymentUid());
            paymentStatusTxService.markCancelFailed(payment.getPaymentUid());
            throw e;
        }

    }

    @Transactional
    public void completeCancelFromWebhook(Payment payment, PortOnePaymentDto portOnePaymentDto) {
        // TODO - 내일 아침에 하기..
        // 이미 취소 완료된 건에 대하여는 멱등성 보장
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return;
        }
        // 2. 상태 검증
        if (!(payment.getPaymentStatus() == PaymentStatus.CANCEL_REQUESTED
                || payment.getPaymentStatus() == PaymentStatus.CANCEL_FAILED)) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 3. paymentUid 검증
        if (!payment.getPaymentUid().equals(portOnePaymentDto.paymentId())) {
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_NOT_EQUALS);
        }
        // 4. 금액 검증
        if (!payment.getFinalAmount().equals(portOnePaymentDto.amount())) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        payment.cancelled();
        // 상태 전이 할 것 없음
    }

    @Transactional
    public void failPendingPaymentFromWebhook(Payment payment, PortOnePaymentDto portOnePaymentDto) {
        // 결제 확정 요청이 오지 않아 pending 상태로 남아있던 결제 건들에 대하여 웹훅으로 미결제 확인 후 결제 실패 처리
        // 이미 결제 실패 처리된 건에 대하여는 멱등성 보장
        if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
            return;
        }
        // 2. 상태 검증
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        // 3. paymentUid 검증
        if (!payment.getPaymentUid().equals(portOnePaymentDto.paymentId())) {
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_NOT_EQUALS);
        }
        payment.failed();
        Order order = payment.getOrder();

        // 가점유 했던 포인트 가점유 해제
        if (payment.getPointToUse() > 0) {
            userPointService.cancelUsePoint(order.getUserId(), order.getId(), payment.getPointToUse());
        }
    }

    // TODO - 주문 금액에 따른 멤버십 등급 자동 업데이트 기능이 있나?

}
