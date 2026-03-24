package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.PortOneException;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.dto.response.ConfirmPaymentResponse;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentCheckResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentStatus;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import static aQute.bnd.annotation.headers.Category.payment;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentVerificationService {   // 포트원 결제 조회 결과만 검증하는 서비스

    private final PortOneService portOneService;

    // 포트원 조회 (재시도 포함 3회)
    public PaymentResult checkPayment(Payment payment) {
        String paymentUid = payment.getPaymentUid();
        PaymentResult paymentResult;
        try {
            PortOnePaymentDto portOnePaymentDto = getPaymentWithRetry(payment.getPaymentUid());
            paymentResult = getPaymentResult(payment, portOnePaymentDto);
            return paymentResult;
        }catch (InterruptedException e) { // 포트원 재조회 과정 중 작업 중단, 결제 실패 처리 후 웹훅 확인해서 결제 되었으면 취소 처리
            Thread.currentThread().interrupt();
            log.warn("PortOne confirm interrupted. paymentUid={}", paymentUid, e);
            paymentResult = PaymentResult.FAIL;
        } catch (RestClientException e) {   // 네트워크 이슈로 결과 조회 불가, 결제 실패 처리 후 웹훅 확인해서 결제 되었으면 취소 처리
            log.warn("PortOne network error. paymentUid={}", paymentUid, e);
            paymentResult = PaymentResult.FAIL;
        } catch (PortOneException e) { // 네트워크 이슈로 결과 조회 불가, 결제 실패 처리 후 웹훅 확인해서 결제 되었으면 취소 처리
            log.warn("PortOne error. paymentUid={}", paymentUid, e);
            paymentResult = PaymentResult.FAIL;
        } catch (RuntimeException e) { // 상태 변경 없이 롤백, 서버 에러 응답, 나중에 운영 로그 확인,
            log.error("Unexpected confirmPayment error. paymentUid={}", paymentUid, e);
            throw e;
        }
        return paymentResult;
    }

    public PortOnePaymentDto getPaymentWithRetry(String paymentUid) throws InterruptedException {

        int retry = 0;

        while (retry < 3) { // 조회 총 3번 시도
            try {
                PortOnePaymentDto portOnePaymentDto = portOneService.getPayment(paymentUid);
                if (portOnePaymentDto.status() == PortOnePaymentStatus.PAID) {
                    return portOnePaymentDto;
                }
                if (portOnePaymentDto.status() == PortOnePaymentStatus.READY
                        || portOnePaymentDto.status() == PortOnePaymentStatus.PAY_PENDING) {
                    retry++;
                    Thread.sleep(1000);
                    continue;
                }
                return portOnePaymentDto;
            } catch (RestClientException e) {
                retry++;
                Thread.sleep(1000); // ⭐ 추가
            } catch (PortOneException e) {
                throw e;
            }
        }
        // 네트워크 에러로 조회 재시도에도 불구하고 조회 실패 시 ServiceException 던지기
        throw new ServiceException(ErrorCode.PORTONE_UNAVAILABLE);
    }
    // 포트원 조회 성공 시 상태 검증
    public PaymentResult getPaymentResult(Payment payment, PortOnePaymentDto portOnePaymentDto) {
        PortOnePaymentStatus status = portOnePaymentDto.status();
        /*
        PortOnePaymentStatus
            PAID,
            FAILED,
            CANCELLED,
            READY,
            PAY_PENDING;
         */
        switch (status) {
            case PAID -> {
                if (!isPaidAmountMatched(payment, portOnePaymentDto)) { // 결제 금액 불일치
                    return PaymentResult.AMOUNT_MISMATCH;
                }
                return PaymentResult.SUCCESS;

            }
            case CANCELLED -> {
                return PaymentResult.CANCELLED;
            }
            default -> {
                return PaymentResult.FAIL;  // 이미 READY와 PAY_PENDING 인 경우 재조회 했기 때문에 결제 실패처리 하기
            }
        }
    }
    // 결제 금액 일치 검증

    private boolean isPaidAmountMatched(Payment payment, PortOnePaymentDto paymentDto) {
        return payment.getFinalAmount().equals(paymentDto.amount());
    }
}