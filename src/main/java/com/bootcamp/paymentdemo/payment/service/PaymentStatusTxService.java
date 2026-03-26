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
import org.springframework.dao.CannotAcquireLockException;
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
        try {
            Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            payment.cancelRequested();
        } catch (CannotAcquireLockException e) {
            log.warn("락 획득 실패 - 이미 처리 중. paymentUid={}", paymentUid);
            return;
        }

    }

    // 결제 취소 요청 성공으로 상태 변경
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelled(String paymentUid) {
        try {
            Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            payment.cancelled();
        } catch (CannotAcquireLockException e) {
            log.warn("락 획득 실패 - 이미 처리 중. paymentUid={}", paymentUid);
            return;
        }

    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelFailed(String paymentUid) {
        try {
            Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            payment.cancelFailed();
        } catch (CannotAcquireLockException e) {
            log.warn("락 획득 실패 - 이미 처리 중. paymentUid={}", paymentUid);
            return;
        }

    }

    // 결제 확정 성공 상태 전이
    @Transactional
    public void markSuccess(String paymentUid) {
    //이미 다른 결제가 처리된 상태면 아무 상태도 섣불리 확정하지 않고 종료
    //로그만 남김
        try {
            Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));

            Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId())
                    .orElseThrow(() -> new ServiceException(ErrorCode.ORDER_NOT_FOUND));

            log.info("markSuccess start. orderId={}, paymentUid={}, paymentStatus={}, orderStatus={}, userId={}",
                    order.getId(),
                    paymentUid,
                    payment.getPaymentStatus(),
                    order.getStatus(),
                    order.getUserId());


            // 1. 같은 payment가 이미 성공 처리된 경우: 멱등 처리
            if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
                log.info("markSuccess skipped - payment already SUCCESS. orderId={}, paymentUid={}",
                        order.getId(), paymentUid);
                return;
            }

            // 2. 이미 다른 결제로 주문이 결제 완료된 경우
            // 현재 payment를 성공 처리하면 안 됨
            if (order.getStatus() == OrderStatus.PAID) {
                log.warn("markSuccess skipped - order already PAID by another payment. orderId={}, paymentUid={}, paymentStatus={}",
                        order.getId(), paymentUid, payment.getPaymentStatus());

                return;
            }

            // 3. 현재 payment는 성공 가능한 상태인지 검증
            if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
                log.warn("markSuccess failed - invalid payment status. orderId={}, paymentUid={}, paymentStatus={}",
                        order.getId(), paymentUid, payment.getPaymentStatus());
                throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS_FOR_REFUND);
            }

            // 4. 주문도 성공 가능한 상태인지 검증
            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("markSuccess failed - invalid order status. orderId={}, paymentUid={}, orderStatus={}",
                        order.getId(), paymentUid, order.getStatus());
                throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
            }

            // 5. 재고 차감
            productService.decreaseStockByOrder(order);
            // 6. 포인트 가점유 해제 부분은 order 확정으로 옮김

            // 7. 상태 전이
            payment.success();
            order.markAsPaid();

            log.info("markSuccess completed. orderId={}, paymentUid={}", order.getId(), paymentUid);
        } catch (CannotAcquireLockException e) {
            log.warn("락 획득 실패 - 이미 처리 중. paymentUid={}", paymentUid);
            return;
        }

    }

    // 결제 확정 실패 상태 전이
    @Transactional
    public void markFailed(String paymentUid){
        try {
            Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                    .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
            Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElseThrow(
                    () -> new ServiceException(ErrorCode.ORDER_NOT_FOUND)
            );
            // 결제 상태 실패로 변경
            payment.failed();
            // 주문 상태는 유지. 호출할 것 없음
            // 포인트 가점유 해제
//            if (payment.getPointToUse() > 0) {
//                userPointService.cancelUsePoint(order.getUserId(), payment.getPointToUse());
//            }
        } catch (CannotAcquireLockException e) {
            log.warn("락 획득 실패 - 이미 처리 중. paymentUid={}", paymentUid);
            return;
        }

    }

}
