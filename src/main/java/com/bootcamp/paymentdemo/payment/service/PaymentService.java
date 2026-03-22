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
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PortOneService portOneService;
    private final UserService userService;

    @Transactional
    public CreatePaymentResponse createPayment(String orderId, CreatePaymentRequest request) {
        Order order = orderService.getOrderByOrderUid(orderId);

        // 주문 상태 검증
        if(order.getStatus()!= OrderStatus.PENDING){
            throw new ServiceException(ErrorCode.ORDER_STATUS_NOT_PENDING);
        }

        // TODO : 결제 생성 시 포인트 있는지 검증.
        Long userId = order.getUserId();
        int currentPoint = userService.getCurrentPoint(userId);
        Integer pointToUse = request.getPointToUse();
        if (currentPoint < pointToUse) {
            throw new ServiceException(ErrorCode.INSUFFICIENT_POINT);
        }

        // 중복 결제 방지
        // 같은 주문에 대해서 결제 요청 후 대기중(PENDING) 상태인 결제가 있는 경우
        boolean existence = paymentRepository.existsByOrderAndPaymentStatus(order, PaymentStatus.PENDING);

        if(existence){
            throw new ServiceException(ErrorCode.ALREADY_PENDING_PAYMENT);
        }

        // 주문 금액 검증
        Long totalAmount = request.getTotalAmount();
        if (totalAmount == null) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        if (!totalAmount.equals(order.getTotalAmount())) { // 결제 요청시 들어온 금액과 주문 금액이 일치하는지 검증
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        // 실제 결제 금액 계산
        Long finalAmount = totalAmount - pointToUse;

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

    /*
    catch (PAYMENT_NOT_FOUND) → PENDING
    catch (PORTONE_SERVER_ERROR) → PENDING
    catch (PORTONE_COMMUNICATION_ERROR) → PENDING
     */
    public ConfirmPaymentResponse completePayment(String paymentUid) {

        // paymentId 검증
        if(!StringUtils.hasLength(paymentUid)){
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_UID);
        }
        // DB에서 Payment 객체 조회. 없으면 생성되지 않은 결제 요청
        Payment payment = paymentRepository.findByPaymentUid(paymentUid).orElseThrow(
                () -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // Payment 객체 상태 검증. 결제 대기 상태가 아니면 이미 결제 완료 처리된 결제 요청
        if(!payment.getPaymentStatus().equals(PaymentStatus.PENDING)){
            throw new ServiceException(ErrorCode.ALREADY_PROCESSED_PAYMENT);
        }

        // 포트원 조회
        PortOnePaymentDto portOnePaymentDto;
        try {
            portOnePaymentDto = portOneService.getPayment(paymentUid);
        } catch (ServiceException e) {
            // TODO : 결제 에러 별 에러 처리 로직 필요
            throw new ServiceException(ErrorCode.PORTONE_PAYMENT_VALIDATION_FAILED);
        }

        // // TODO : 포트원 데이터 결제 상태 검증, FAILED 일 때와 CANCELLED일 때도 생각해서 구현해야함
        if(PortOnePaymentStatus.READY.equals(portOnePaymentDto.status())){
            throw new ServiceException(ErrorCode.NOT_PAID_YET);
        }


        // 포트원 데이터 결제금액과 Payment 객체 결제 금액 동일한지 검증
        if(!payment.getAmount().equals((long) portOnePaymentDto.paid())){
            // TODO : 포트원에서 결제는 되었는데 결제 금액이 동일하지 않으면 결제 취소 요청을 보내기..
            portOneService.cancelPayment(paymentUid);
            throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_NOT_EQUALS);
        }

        // TODO : Webhook 확인.. 현민님께서 담당하기로 했는데.. 일단 저도 공부해서 구현해보는 쪽으로 할까요?

        // Payment 객체 상태값 갱신
        payment.paid();

        // TODO 결제 완료 시 주문, 유저, 포인트 쪽에 전달해줘야 함.


        return ConfirmPaymentResponse.of(payment.getOrder().getOrderUid(), portOnePaymentDto);
    }

    private static String createPaymentId() {
        return "PAY-" + UUID.randomUUID();
    }

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        }
    }
