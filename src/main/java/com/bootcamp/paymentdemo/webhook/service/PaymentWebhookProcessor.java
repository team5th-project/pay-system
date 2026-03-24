package com.bootcamp.paymentdemo.webhook.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.payment.service.PortOneService;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import com.bootcamp.paymentdemo.webhook.dto.PortOneWebhookRequest;
import com.bootcamp.paymentdemo.webhook.enums.PortOneEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentWebhookProcessor {

    private final PortOneService portOneService;
    private final PaymentService paymentService;
    private final RefundService refundService;

    @Transactional
    public void process(PortOneWebhookRequest request) {
        PortOneEventType eventType = PortOneEventType.from_(request.type())
                .orElseThrow(() -> new ServiceException(ErrorCode.INVALID_WEBHOOK_EVENT));

        String paymentUid = request.data().paymentId();

        Payment payment = paymentService.getPaymentByUid(paymentUid);
        PortOnePaymentDto portOnePayment = portOneService.getPayment(paymentUid);

        switch (eventType) {
            case PAID -> processPaid(payment, portOnePayment);
            case CANCELLED -> processCancelled(payment, portOnePayment);
        }
    }

    // 웹훅의 실제 결제가 완료되어 있는 경우
    private void processPaid(Payment payment, PortOnePaymentDto portOnePayment) {
        if (portOnePayment.status() != PortOnePaymentStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_STATUS);
        }

        // =============== 결제 쪽 사용 ============

        // 결제 성공처리 요청
        // 1. 결제 확정 요청이 오지 않아서 PENDING 상태인 결제들을 SUCCESS 처리
        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            paymentService.completePaymentFromWebhook(payment, portOnePayment);
            return;
        }

        // 결제 취소 요청
        // 1. 실제 결제가 되었는데 네트워크 오류로 payment는 실패 처리 되어있는 경우
        // 2. 서버 내부 오류로 결제 취소 요청을 보냈는데, 취소가 되지 않은 경우
        if (payment.getPaymentStatus() == PaymentStatus.FAILED
        || payment.getPaymentStatus() == PaymentStatus.CANCEL_REQUESTED
        || payment.getPaymentStatus() == PaymentStatus.CANCEL_FAILED) {
            paymentService.requestCancel(payment, portOnePayment);
            return;
        }

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return; // 멱등 처리
        }

        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return;
        }

        // ====================================
        /*
        TODO : 현민님 여기 밑에 환불쪽에서 필요한 부분 추가해서 사용하면 됩니다.
         */
        // ======= 환불 쪽 사용 ================

        throw new ServiceException(ErrorCode.INVALID_WEBHOOK_STATUS);

    }

    // 웹훅의 실제 결제가 cancelled 인 경우
    private void processCancelled(Payment payment, PortOnePaymentDto portOnePayment) {

        if (portOnePayment.status() != PortOnePaymentStatus.CANCELLED) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_STATUS);
        }

        // =============== 결제 쪽 사용 ============

        // 우리 payment의 결제 상태가 CANCEL_REQUESTED, CANCEL_FAILED 인 경우는 CANCELLED 처리
        // 1. 취소 요청되었던 결제 건 → 취소 완료 확정
        if (payment.getPaymentStatus() == PaymentStatus.CANCEL_REQUESTED
                || payment.getPaymentStatus() == PaymentStatus.CANCEL_FAILED) {

            paymentService.completeCancelFromWebhook(payment, portOnePayment);
            return;
        }

        // 2. PENDING 상태였던 결제들은 FAILED 처리
        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            paymentService.failPendingPaymentFromWebhook(payment, portOnePayment);
            return;
        }

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return; // TODO 아마 현민님 여기다가 코드 추가해야 할 일이 있을 것 같습니다.
            // 로직 검증해보고 수정해주세욥. 쓸 일 없으시면 그대로 두면 됩니다.
        }
        // ====================================
        /*
        TODO : 현민님 여기 밑에 환불쪽에서 필요한 부분 추가해서 사용하면 됩니다.
         */
        // ======= 환불 쪽 사용 ================


        return;
    }
}