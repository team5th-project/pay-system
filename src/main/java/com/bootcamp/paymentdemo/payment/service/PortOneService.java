package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.PortOneException;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.dto.response.PortOneResponse;
import com.bootcamp.paymentdemo.refund.dto.request.PortOneCancelRequest;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancelResponse;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancellationDto;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortOneService {
    private final RestClient portOneRestClient;
    private final ObjectMapper objectMapper;

    public PortOneCancellationDto cancelPayment(String paymentUid, String reason) {
        try {
            System.out.println("PortOneService.cancelPayment");
            PortOneCancelRequest request = PortOneCancelRequest.of(reason);

            PortOneCancelResponse response = portOneRestClient.post()
                    .uri("/payments/{paymentUid}/cancel", paymentUid)
                    .body(request)
                    .retrieve()
                    .body(PortOneCancelResponse.class);

            if (response == null) {
                log.error("PortOne cancel response is null. paymentUid={}", paymentUid);
                throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }

            PortOneCancellationDto cancellation = response.cancellation();

            if (cancellation == null || cancellation.status() == null) {
                log.error("PortOne cancellation response is invalid. paymentUid={}", paymentUid);
                throw new ServiceException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }

            return cancellation;

        } catch (ServiceException e) {
            throw e;

        } catch (RestClientResponseException e) {
            log.error("PortOne cancel failed. paymentUid={}, status={}, responseBody={}",
                    paymentUid, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw mapPortOneCancelError(e);

        } catch (RestClientException e) {
            log.error("PortOne cancel communication failed. paymentUid={}", paymentUid, e);
            throw new ServiceException(ErrorCode.PORTONE_COMMUNICATION_ERROR);
        }
    }

    public PortOnePaymentDto getPayment(String paymentUid){
        try {
            PortOneResponse portOneResponse = portOneRestClient.get()
                    .uri("/payments/{paymentUid}", paymentUid) // RestClientConfig에 설정해놓은 baseUrl 뒤에 붙을 경로

                    .retrieve() // 응답이 오면 기본적으로 예외 발생
                    // 특정 상태코드일 때 커스텀 예외 처리 가능
                    .onStatus(status -> status.is4xxClientError(),  // 4xx 에러인 경우 예외처리
                            ((request, response) ->
                            {
                                int errorcode = response.getStatusCode().value();
                                log.error("Portone 4xx Error - status : {}, paymentUid : {}", errorcode, paymentUid);
                                if (errorcode == 400) { // paymentId 또는 요청 형식 문제. 사용자에게 내려주면 안됨
                                    throw new PortOneException(ErrorCode.INVALID_PAYMENT_VALIDATION_REQUEST);
                                } else if (errorcode == 401 || errorcode == 403) { // 인증 또는 권한 문제 (Secret Key)
                                    throw new PortOneException(ErrorCode.UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST);
                                } else if (errorcode == 404) {  // 1. 클라이언트 조작 가능성 2. PG -> 포트원 동기화 지연 가능성
                                    throw new PortOneException(ErrorCode.PORTONE_PAYMENT_NOT_FOUND);
                                } else {
                                    throw new PortOneException(ErrorCode.PORTONE_UNKNOWN_ERROR);
                                }
                            })
                    )
                    .onStatus(status -> status.is5xxServerError(), // 5xx 에러인 경우 예외처리
                            ((request, response) ->
                            {
                                int errorcode = response.getStatusCode().value();
                                log.error("Portone 5xx Error : status : {}, paymentUid : {}", errorcode, paymentUid);
                                throw new ServiceException(ErrorCode.PORTONE_SERVER_ERROR);
                            }))
                    .body(PortOneResponse.class);   // JSON 반환값을 Response Dto로 역직렬화(Deserialization)
                    /*
                    데이터 직렬화 : 메모리를 디스크에 저장하거나, 네트워크 통신에 사용하기 위한 형식으로 변환하는 것이다.
                    데이터 역직렬화 : 디스크에 저장한 데이터를 읽거나, 네트워크 통신으로 받은 데이터를 메모리에 쓸 수 있도록 변환하는 것이다.
                     */
            if (portOneResponse == null) {
                throw new ServiceException(ErrorCode.PORTONE_UNAVAILABLE);
            }

            return PortOnePaymentDto.from(portOneResponse);

        } catch (RestClientException | ServiceException  e) { // 네트워크 및 기타 예외
            log.error("PortOne 통신 실패 - {}", e.getMessage());
            throw e;
        }
    }

    private RuntimeException mapPortOneCancelError(RestClientResponseException e) {
        PortOneErrorResponse error = parseErrorBody(e.getResponseBodyAsString());
        String type = error != null ? error.type() : null;
        int status = e.getStatusCode().value();

        if (status == 400) {
            return new PortOneException(ErrorCode.INVALID_PAYMENT_VALIDATION_REQUEST);
        }

        if (status == 401 || status == 403) {
            return new PortOneException(ErrorCode.UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST);
        }

        if (status == 404) {
            return new PortOneException(ErrorCode.PORTONE_PAYMENT_NOT_FOUND);
        }

        if (status == 409) {
            if ("PAYMENT_NOT_PAID".equalsIgnoreCase(type)) {
                return new PortOneException(ErrorCode.INVALID_PAYMENT_STATUS);
            }
            if ("PAYMENT_ALREADY_CANCELLED".equalsIgnoreCase(type)) {
                return new PortOneException(ErrorCode.INVALID_REFUND_STATUS);
            }
            return new PortOneException(ErrorCode.REFUND_FAILED);
        }
        if (e.getStatusCode().is5xxServerError()) {
            return new ServiceException(ErrorCode.PORTONE_SERVER_ERROR);
        }

        return new PortOneException(ErrorCode.PORTONE_UNKNOWN_ERROR);
    }

    private PortOneErrorResponse parseErrorBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, PortOneErrorResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse PortOne error body. responseBody={}", responseBody, e);
            return null;
        }
    }
}
