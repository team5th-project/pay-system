package com.bootcamp.paymentdemo.webhook.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.payment.service.PortOneService;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import com.bootcamp.paymentdemo.refund.service.PortOneRefundService;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import com.bootcamp.paymentdemo.webhook.dto.PortOneWebhookRequest;
import com.bootcamp.paymentdemo.webhook.entity.PortOneEventType;
import com.bootcamp.paymentdemo.webhook.entity.WebhookEvent;
import com.bootcamp.paymentdemo.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WebhookEventService {
    private final WebhookEventRepository webhookEventRepository;
    private final PortOneService portOneService;
    private final PaymentService paymentService;
    private final RefundService refundService;

    public void handleWebhook(String webhookId, String signature, String timestamp, PortOneWebhookRequest request) {
        // 포트원에서 보낸 요청이 맞는지 검증
        // 헤더 값이 비어있는지 확인
        if (webhookId == null || webhookId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_WEBHOOKID);
        }
        if (signature == null || signature.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
        }
        validateTimestampRange(timestamp);

        // 웹훅 안에서 paymentId, eventType 추출
        String eventType = request.type();
        String paymentUid = request.data().paymentId();
        // eventType enum 변환
        PortOneEventType portOneEventType = PortOneEventType.from(eventType);

        // 멱등성 체크
        // 외부 시스템은 같은 웹훅을 재전송할 수 잇음
        if (webhookEventRepository.existsByWebhookId(webhookId)) {
            return; // 이미 처리된 웹훅이면 종료
        }


        // 웹훅 기록 저장
        // 아직 처리 성공/실패는 모르고 일단 받은 상태
        WebhookEvent webhookEvent = WebhookEvent.createReceive(
                webhookId,
                paymentUid,
                portOneEventType,
                null
        );
        webhookEventRepository.save(webhookEvent);
        try {
            // 3. paymentId로 포트원 실제 결제/취소 정보 재조회
            PortOnePaymentDto providerPayment = portOneService.getPayment(paymentUid);

            // 우리 DB의 외부식별자 조회
            Payment payment = paymentService.getPaymentById(paymentUid);
            Order order = payment.getOrder();

            // PortOne 정보와 우리 DB 결제정보가 맞는지 검증
            if (!providerPayment.paymentId().equals(payment.getPaymentUid())) {
                throw new ServiceException(ErrorCode.INVALID_PAYMENT_UID);
            }
            // PortOne 정보와 우리 DB 결제정보의 금액검증
            if ((!Objects.equals(providerPayment.amount(), payment.getFinalAmount()))) {
                throw new ServiceException(ErrorCode.PAYMENT_AMOUNT_NOT_EQUALS);
            }

            // 결제 성공 이벤트
            if (portOneEventType == PortOneEventType.PAID) {
                // payment/order 상태를 성공쪽으로 변경
                handlePaidEvent(providerPayment, payment, order);

                // 환불 성공 이벤트
            } else if (portOneEventType == PortOneEventType.CANCELLED) {
                // 환불 완료 처리 또는 취소 상태 반영
                handleCancelledEvent(providerPayment, payment, order);
                // TODO Transaction.Failed를 추가할까?
            } else {
                //지금 처리 대상이 아닌 이벤트는 무시
                log.info("웹훅 이벤트 무시됨 = {}, eventType={}", webhookId ,eventType);
            }
            // 여기까지 문제없으면 웹훅 처리 성공으로 변경
            webhookEvent.markProcessed();

        } catch (ServiceException e) {
            log.warn("웹훅 비즈니스 처리 실패 webhookId={}, paymentUid={}, eventType={}, reason={}",
                    webhookId, paymentUid, eventType, e.getMessage());
            webhookEvent.markFailed(resolveFailureReason(e));
            throw e;
        } catch (Exception e) {
            log.error("웹훅 처리 중 서버 오류 발생 webhookId={}, paymentUid={}, eventType={}",
                    webhookId, paymentUid, eventType, e);
            webhookEvent.markFailed(resolveFailureReason(e));
            throw new ServiceException(ErrorCode.PORTONE_SERVER_ERROR);
        }
    }

    private void handlePaidEvent(PortOnePaymentDto providerPayment, Payment payment, Order order) {

        // 1. PortOne 최종 상태가 진짜 결제 완료 상태인지 확인
        if (providerPayment.status() != PortOnePaymentStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 2. 이미 성공 처리된 결제면 중복 처리하지 않음
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        // 3. 성공으로 상태 전이 가능한 결제 상태인지 확인
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 4. 주문 상태도 결제 완료로 바꿔도 되는지 확인
        // 주문 enum은 네 프로젝트에 맞게 바꿔라.
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 5. 상태 전이
        payment.success();
        order.markAsPaid();
    }

    private void handleCancelledEvent(PortOnePaymentDto providerPayment, Payment payment, Order order) {

        // PortOne 최종 상태가 진짜 취소/환불 완료 상태인지 확인
        if (providerPayment.status() != PortOnePaymentStatus.CANCELLED) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 이미 환불 완료된 결제면 중복 처리하지 않음
        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        // 원래 성공했던 결제만 환불 가능
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 환불 엔티티 조회
        Refund refund = refundService.getRefundByPaymentUid(payment.getPaymentUid());

        // 이미 완료된 건일 시 중복처리 방지
        if (refund.getRefundStatus() == RefundStatus.COMPLETED) {
            return;
        }
        // 환불 가능한 상태인지 확인
        if (refund.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new ServiceException(ErrorCode.INVALID_REFUND_STATUS);
        }

        // PortOne이 실제로 취소한 금액과 우리가 환불하려던 금액을 검증
        Long cancelAmount = providerPayment.amount(); // PortOne이 실제 환불하려는 금액
        Long refundAmount = refund.getPayment().getFinalAmount(); // 우리DB가 알고있는 결제금액

        // 환불 금액 검증
        if (!cancelAmount.equals(refundAmount)) {
            throw new ServiceException(ErrorCode.INVALID_REFUND_AMOUNT);
        }

        // 주문 상태도 환불/취소로 전이 가능한지 확인
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ServiceException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 8. 상태 전이
        payment.refund();
        refund.complete();
        order.refund();
    }

    private void validateTimestampRange(String timestamp) {
        try {
            // 문자열 timestamp를 시간 객체로 변환
            OffsetDateTime webhookTime = OffsetDateTime.parse(timestamp);
            // 현재 시간을 UTC 기준으로 가져옴
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            // 두 시간 차이를 계산 webhookTime -> now 사이 시간 차이
            long diffMinutes = Math.abs(Duration.between(webhookTime, now).toMinutes());
            // 5분 이내에 온거면 OK 5분 넘었으면 위험하니까 거부
            if (diffMinutes > 5) {
                throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
            }
        } catch (DateTimeParseException e) {
            // timestamp 형식 자체가 이상하면 실패
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
        }
    }

    private String resolveFailureReason(Exception e) {
        String reason;
        if (e instanceof ServiceException se) {
            reason = se.getErrorCode().getMessage();
        } else {
            return "예상치 못한 서버 오류";
        }
        if (reason == null || reason.isBlank()) {
            reason = "알 수 없는 오류";
        }
        return reason.length() > 255 ? reason.substring(0, 255) : reason;
    }
}