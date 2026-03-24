package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.PortOneException;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.enums.PaymentCancelResult;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancellationDto;
import com.bootcamp.paymentdemo.refund.enums.PortOneRefundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancelService {
    private final PortOneService portOneService;

    public PaymentCancelResult processPaymentCancel(Payment payment,String reason) {
        String paymentUid = payment.getPaymentUid();
        PaymentCancelResult cancelResult;
        try{
            PortOneCancellationDto cancellationDto = tryPaymentCancelWithRetry(paymentUid, reason);
            cancelResult = getPaymentCancelResult(payment, cancellationDto);

        }catch (InterruptedException e) { // 포트원 재조회 과정 중 작업 중단, 결제 실패 처리 후 웹훅 확인해서 결제 되었으면 취소 처리
            Thread.currentThread().interrupt();
            log.warn("PortOne confirm interrupted. paymentUid={}", paymentUid, e);
            cancelResult = PaymentCancelResult.CANCEL_REQUESTED;
        } catch (RestClientException e) {   // 네트워크 이슈로 결과 조회 불가, 결제 실패 처리 후 웹훅 확인해서 결제 되었으면 취소 처리
            log.warn("PortOne network error. paymentUid={}", paymentUid, e);
            cancelResult = PaymentCancelResult.CANCEL_REQUESTED;
        } catch (PortOneException e) { // 네트워크 이슈로 결과 조회 불가, 결제 실패 처리 후 웹훅 확인해서 결제 되었으면 취소 처리
            log.warn("PortOne error. paymentUid={}", paymentUid, e);
            cancelResult = PaymentCancelResult.CANCEL_REQUESTED;
        } catch (RuntimeException e) { // 상태 변경 없이 롤백, 서버 에러 응답, 나중에 운영 로그 확인,
            log.error("Unexpected cancelPayment error. paymentUid={}", paymentUid, e);
            throw e;
        }

        return cancelResult;
    }


    public PortOneCancellationDto tryPaymentCancelWithRetry(String paymentUid,String reason) throws InterruptedException{
        int retry = 0;

        while (retry < 3) { // 조회 총 3번 시도
            try {
                PortOneCancellationDto portOneCancellationDto = portOneService.cancelPayment(paymentUid,reason);
                PortOneRefundStatus status = portOneCancellationDto.status();
                if (PortOneRefundStatus.SUCCEEDED.equals(status)) {
                    return portOneCancellationDto;
                }
                if (PortOneRefundStatus.REQUESTED.equals(status)){
                    retry++;
                    Thread.sleep(1000);
                    continue;
                }
                return portOneCancellationDto;
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

    // 포트원 취소 요청 응답 받을 시 상태 겁증
    public PaymentCancelResult getPaymentCancelResult(Payment payment, PortOneCancellationDto portOneCancellationDto) {


        PortOneRefundStatus status = portOneCancellationDto.status();
        /*
        PortOneRefundStatus
        FAILED,
        REQUESTED,
        SUCCEEDED,
        UNKNOWN //
         */
        return switch (status) {
            case SUCCEEDED -> PaymentCancelResult.SUCCESS;
            case REQUESTED -> PaymentCancelResult.CANCEL_REQUESTED;
            default ->  PaymentCancelResult.FAIL;
        };
    }
}
