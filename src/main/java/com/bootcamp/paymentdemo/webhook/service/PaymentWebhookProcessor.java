package com.bootcamp.paymentdemo.webhook.service;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookProcessor {

    private final PortOneService portOneService;
    private final PaymentService paymentService;
    private final RefundService refundService;

    @Transactional
    public void process(PortOneWebhookRequest request, PortOneEventType portOneEventType) {

        String paymentUid = request.data().paymentId();

        Payment payment = paymentService.getPaymentByUid(paymentUid);
        PortOnePaymentDto portOnePayment = portOneService.getPayment(paymentUid);

        switch (portOneEventType) {
            case PAID -> processPaid(payment, portOnePayment);
            case CANCELLED -> processCancelled(payment, portOnePayment);
        }
    }

    // 웹훅의 실제 결제가 완료되어 있는 경우
    private void processPaid(Payment payment, PortOnePaymentDto portOnePayment) {
        if (portOnePayment.status() != PortOnePaymentStatus.PAID) {
            log.warn("웹훅 상태 불일치 (PAID 아님) - paymentId={}, status={}",
                    payment.getPaymentUid(), portOnePayment.status());
            return;
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
            paymentService.requestCancelFromWebhook(payment, portOnePayment);
            return;
        }


        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return; // 멱등 처리
        }

        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return;
        }
        // ======= 환불 쪽 사용 ================

        log.warn("처리되지 않은 웹훅 상태 - paymentId={}, status={}",
                payment.getPaymentUid(), payment.getPaymentStatus());
        return;
    }

    // 웹훅의 실제 결제가 cancelled 인 경우
    private void processCancelled(Payment payment, PortOnePaymentDto portOnePayment) {

        if (portOnePayment.status() != PortOnePaymentStatus.CANCELLED) {
            log.warn("웹훅 상태 불일치 (CANCELLED 아님) - paymentId={}, status={}",
                    payment.getPaymentUid(), portOnePayment.status());
            return;
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

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS
                || payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("환불 웹훅 처리 분기 진입 - paymentUid={}, paymentId={}, paymentStatus={}",
                    payment.getPaymentUid(), payment.getId(), payment.getPaymentStatus());
            refundService.processRefundWebhook(payment.getId());
            return;
        }
        // 4. 이미 취소 완료된 건이면 멱등 처리
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            log.info("이미 취소 완료된 payment 멱등 처리 - paymentId={}", payment.getPaymentUid());
            return;
        }
        log.warn("처리되지 않은 CANCELLED 웹훅 상태 - paymentId={}, status={}",
                payment.getPaymentUid(), payment.getPaymentStatus());
    }
}