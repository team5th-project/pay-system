package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import com.bootcamp.paymentdemo.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentStatusTxService {
    private final PaymentRepository paymentRepository;
    private final ProductService productService;
    private final UserPointService userPointService;
    private final OrderRepository orderRepository;


    // 결제 취소 요청 상태로 변경
    // 현재 진행 중인 트랜잭션을 잠시 멈추고, 완전히 새로운 트랜잭션을 시작하는 옵션. 재고 취소 요청에 실패하더라도 취소 요청했다는 기록은 롤백되면 안되기 때문.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelRequested(String paymentUid) {
        Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.cancelRequested();
    }

    // 결제 취소 요청 성공으로 상태 변경
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelled(String paymentUid) {
        Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.cancelled();
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelFailed(String paymentUid) {

        Payment payment = paymentRepository.findByPaymentUid(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.cancelFailed();
    }

    // 결제 확정 성공 상태 전이
    @Transactional
    public void markSuccess(String paymentUid){
        Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElseThrow(
                () -> new ServiceException(ErrorCode.ORDER_NOT_FOUND)
        );
        log.info("order id={}", order.getId());
        log.info("order status={}", order.getStatus());
        log.info("order userId={}", order.getUserId());
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 재고 차감
        productService.decreaseStockByOrder(order);

        // 포인트 차감
        if (payment.getPointToUse() > 0) {
            userPointService.usePoint(order.getUserId(), order.getId(), payment.getPointToUse());
        }
        // 결제 상태 성공으로 변경
        payment.success();
        // 주문 상태 결제 성공으로 변경
        order.markAsPaid();
    }

    // 결제 확정 실패 상태 전이
    @Transactional
    public void markFailed(String paymentUid){
        Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElseThrow(
                () -> new ServiceException(ErrorCode.ORDER_NOT_FOUND)
        );
        // 결제 상태 실패로 변경
        payment.failed();
        // 주문 상태는 PENDING으로 유지. 호출할 것 없음
        // 포인트 가점유 해제
        if (payment.getPointToUse() > 0) {
            userPointService.cancelUsePoint(order.getUserId(), order.getId(), payment.getPointToUse());
        }
    }

}
