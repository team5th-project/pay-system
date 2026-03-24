package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.PortOneException;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.enums.PaymentCheckResult;
import com.bootcamp.paymentdemo.payment.enums.PaymentResult;
import com.bootcamp.paymentdemo.payment.enums.PortOnePaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class PaymentVerificationService {   // 포트원 결제 조회 결과만 검증하는 서비스

    private final PortOneService portOneService;

    // 포트원 조회 (재시도 포함 3회)
    public PortOnePaymentDto getPaymentWithRetry(String paymentUid) throws InterruptedException {

        int retry = 0;

        while (retry < 3) { // 조회 총 3번 시도
            try {
                PortOnePaymentDto payment = portOneService.getPayment(paymentUid);
                if (payment.status() == PortOnePaymentStatus.PAID) {
                    return payment;
                }
                if (payment.status() == PortOnePaymentStatus.READY
                        || payment.status() == PortOnePaymentStatus.PAY_PENDING) {
                    retry++;
                    Thread.sleep(1000);
                    continue;
                }
                return payment;
            } catch (RestClientException e) {
                retry++;
                Thread.sleep(1000); // ⭐ 추가
            } catch (PortOneException e) {
                throw e;
            }
        }
        // 네트워크 에러로 조회 재시도에도 불구하고 조회 실패 시 ServiceException 던지기
        throw new ServiceException(ErrorCode.PORTONE_UNAVAILABLE);    }

    // 포트원 조회 성공 시 상태 검증
    public PaymentResult getPaymentResult(PortOnePaymentDto portOnePaymentDto) {
        PortOnePaymentStatus status = portOnePaymentDto.status();
        return switch (status) {
            case PAID -> PaymentResult.SUCCESS;
            case READY, PAY_PENDING -> PaymentResult.PENDING;
            default -> PaymentResult.FAIL;
        };
    }


    public PaymentCheckResult checkPayment(String paymentUid) throws InterruptedException {
        PortOnePaymentDto dto = getPaymentWithRetry(paymentUid);
        PaymentResult result = getPaymentResult(dto);
        return new PaymentCheckResult(result, dto);
    }
}