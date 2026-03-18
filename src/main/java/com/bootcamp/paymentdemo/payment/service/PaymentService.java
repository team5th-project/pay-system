package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.payment.dto.request.CreatePaymentRequest;
import com.bootcamp.paymentdemo.payment.dto.response.CreatePaymentResponse;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    @Transactional
    public CreatePaymentResponse createPayment(String orderId, CreatePaymentRequest request) {
        Order order = orderService.getOrderByOrderUid(orderId);

        // 주문 상태 검증
        if(order.getStatus()!= OrderStatus.PENDING){
            throw new ServiceException(ErrorCode.ORDER_STATUS_NOT_PENDING);
        }

        // 중복 결제 방지
        // 같은 주문에 대해서 결제 요청 후 대기중(PENDING) 상태인 결제가 있는 경우
        boolean existence = paymentRepository.existsByOrderAndPaymentStatus(order, PaymentStatus.PENDING);

        if(existence){
            throw new ServiceException(ErrorCode.ALREADY_PENDING_PAYMENT);
        }

        // 결제 금액 검증
        Long totalAmount = request.getTotalAmount();
        if (totalAmount == null) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

//         클라이언트 측에서 보낸 결제 금액과, order 테이블의 결제 금액이 같은지 검증
//         TODO : Order에 point 관련 코드 추가 후
//         방법 1. Order 엔티티에 finalAmount를 추가해서
//         getTotalAmount 대신 실재 결제금액 반환 메서드로 변경하기.
//         방법 2. Order에 getTotalAmount와 getUsedPoint 추가해서
//         결제 쪽 테이블에 finalAmount 계산 후 저장
        if (!totalAmount.equals(order.getTotalAmount())) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }

        Payment payment = Payment.builder()
                .paymentUid(createPaymentId())
                .order(order)
                .amount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        return CreatePaymentResponse.from(payment);
    }

    private static String createPaymentId() {
        return "PAY-" + UUID.randomUUID();
    }
}
