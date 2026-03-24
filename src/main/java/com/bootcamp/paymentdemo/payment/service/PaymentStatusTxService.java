package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.respository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentStatusTxService {
    private final PaymentRepository paymentRepository;

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

//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    public void markRefunded(String paymentUid){
//        Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
//                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
//        payment.refund();
//    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelFailed(String paymentUid) {
        Payment payment = paymentRepository.findByPaymentUidForUpdate(paymentUid)
                .orElseThrow(() -> new ServiceException(ErrorCode.PAYMENT_NOT_FOUND));
        payment.cancelFailed();
    }


}
