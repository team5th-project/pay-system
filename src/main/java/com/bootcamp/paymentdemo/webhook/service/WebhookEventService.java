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
import com.bootcamp.paymentdemo.refund.service.RefundService;
import com.bootcamp.paymentdemo.webhook.dto.PortOneWebhookRequest;
import com.bootcamp.paymentdemo.webhook.entity.PortOneEventType;
import com.bootcamp.paymentdemo.webhook.entity.WebhookEvent;
import com.bootcamp.paymentdemo.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long ALLOWED_TIMESTAMP_SKEW_SECONDS = 300L; // 허용할 시간 차이 = 300초(5분)
    private static final String HMAC_SHA256 = "HmacSHA256";
    @Value("${portone.webhook.secret}")
    private String webhookSecret;

    public void handleWebhook(String webhookId, String signature, String timestamp, String rawPayload) {
        log.info("handleWebhook start");
        log.info("webhookId = [{}]", webhookId);
        log.info("timestamp = [{}]", timestamp);
        log.info("rawPayload = [{}]", rawPayload);
        log.info("received signature = [{}]", signature);
        // 헤더/바디 값 체크
        validateHeaders(webhookId, signature, timestamp, rawPayload);
        // 재전송 공격 방지 timestamp 검증
        validateTimestampRange(timestamp);
        // PortOne 서명 실검증
        verifyWebhook(webhookId, signature, timestamp, rawPayload);
        // 문자열 JSON을 DTO로 변환
        PortOneWebhookRequest request = parsePayload(rawPayload);
        // 변환된 DTO 필수값 검증
        validateParsedRequest(request);

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
                rawPayload
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
                log.info("웹훅 이벤트 무시됨 = {}, eventType={}", webhookId, eventType);
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
            long webhookEpochSeconds = Long.parseLong(timestamp); // 요청이 온 시간
            long nowEpochSeconds = Instant.now().getEpochSecond(); // 지금 시간
            long diffSeconds = Math.abs(nowEpochSeconds - webhookEpochSeconds); // 지금 시간 - 요청이 온 시간

            if (diffSeconds > ALLOWED_TIMESTAMP_SKEW_SECONDS) { // diffseconds > 300초 넘으면 거부해버림
                throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
            }
        } catch (NumberFormatException e) {
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

    private PortOneWebhookRequest parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, PortOneWebhookRequest.class);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
    }

    private void validateHeaders(String webhookId, String signature, String timestamp, String rawPayload) {
        if (webhookId == null || webhookId.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_WEBHOOKID);
        }
        if (signature == null || signature.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
        if (timestamp == null || timestamp.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_TIMESTAMP);
        }
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
    }

    private void verifyWebhook(String webhookId, String signature, String timestamp, String rawPayload) {
        try {
            if (webhookSecret == null || webhookSecret.isBlank()) {
                throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
            }

            // 우리만 알고있는 비밀키 준비
            byte[] secretBytes = decodeWebhookSecret(webhookSecret);
            // 서명 메세지 만들기/portone도 이 문자열로 서명 작성
            String signedContent = webhookId + "." + timestamp + "." + rawPayload;

            // PortOne이 했던 방식 그대로 우리가 다시 계산 / HMAC-SHA256 계산 후 base64 인코딩
            String expectedSignature = calculateHmacBase64(secretBytes, signedContent);

            // 받은 서명 다시 꺼내기
            String[] signatureEntries = signature.trim().split("\\s+");

            boolean matched = false;
            for (String entry : signatureEntries) {
                String[] parts = entry.split(",", 2);
                if (parts.length != 2) {
                    continue;
                }

                String version = parts[0].trim();
                String providedSignature = parts[1].trim();

                // 대칭 서명만 처리 (문서 기준 v1 = HMAC-SHA256)
                if (!"v1".equals(version)) {
                    continue;
                }

                byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
                byte[] providedBytes = providedSignature.getBytes(StandardCharsets.UTF_8);

                // 상수 시간 비교 / 내가 만든 사인이 받은 사인과 같은가?
                if (MessageDigest.isEqual(expectedBytes, providedBytes)) {
                    matched = true;
                    break;
                }
            }
            // 하나라도 맞으면 OK 하나도 없으면 차단!
            if (!matched) {
                throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }

    private byte[] decodeWebhookSecret(String webhookSecret) {
        String normalized = webhookSecret.trim();
        if (normalized.startsWith("whsec_")) {
            normalized = normalized.substring("whsec_".length());
        }

        try {
            return Base64.getDecoder().decode(normalized); // 암호화된 문자열 -> 실제 키로 return
        } catch (IllegalArgumentException e) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }

    private String calculateHmacBase64(byte[] secretBytes, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256); // 암호 계산기 준비
            SecretKeySpec keySpec = new SecretKeySpec(secretBytes, HMAC_SHA256);
            mac.init(keySpec); // 우리의 비밀키값 넣기

            byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8)); // 문자열에 사인 찍기
            return Base64.getEncoder().encodeToString(hmac); // 사람이 비교할 수 있도록 문자열로 변환
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }

    private void validateParsedRequest(PortOneWebhookRequest request) {
        // 파싱은 됐지만 request 자체가 비어있을 수 있음
        if (request == null) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
        // 이벤트타입이 없으면 이후 enum 변환 자체가 불가능
        if (request.type() == null || request.type().isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
        // data 객체가 없거나 paymentId 가 비어있으면 어떤 결제건인지 식별 불가능
        if (request.data() == null || request.data().paymentId() == null || request.data().paymentId().isBlank()) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_PAYLOAD);
        }
    }
}