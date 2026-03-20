package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.refund.dto.request.PortOneCancelRequest;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancelResponse;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneCancellationDto;
import com.bootcamp.paymentdemo.refund.dto.response.PortOneErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortOneRefundService {

    @Qualifier("portOneRestClient")
    private final RestClient portOneRestClient;
    private final ObjectMapper objectMapper;

    public PortOneCancellationDto cancelPayment(String paymentUid, String reason) {
        try {
            // PortOne 취소 요청 DTO 생성
            PortOneCancelRequest request = PortOneCancelRequest.of(reason);

            //외부 API 호출
            PortOneCancelResponse response = portOneRestClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentUid)
                    .body(request)
                    .retrieve()
                    .body(PortOneCancelResponse.class);

            // 응답 DTO 받기
            if (response == null || response.cancellation() == null) {
                log.error("PortOne 환불 응답이 비어 있습니다. paymentUid : {}", paymentUid);
                throw new ServiceException(ErrorCode.REFUND_FAILED);
            }
            return response.cancellation();
            // 의도적으로 만든 예외는 그대로 통과
        } catch (ServiceException e) {
            throw e;

        } catch (RestClientResponseException e) {
            log.error("PortOne 환불 요청 실패 paymentUid : {}, status : {}, responseBody : {}"
                    , paymentUid, e.getStatusCode(), e.getResponseBodyAsString(), e);
            // PortOne 외부 에러처리
            throw mapPortOneError(e);

            // 기타 외부 에러
        } catch (Exception e) {
            log.error("PortOne 환불 요청 중 알 수 없는 오류 paymentUid : {}", paymentUid, e);
            throw new ServiceException(ErrorCode.PORTONE_COMMUNICATION_ERROR);
        }
    }

    private ServiceException mapPortOneError(RestClientResponseException e) {
        PortOneErrorResponse error = parseErrorBody(e.getResponseBodyAsString());
        String type = error != null ? error.type() : null;


        if (e.getStatusCode().value() == 401) {
            return new ServiceException(ErrorCode.UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST);
        }
        if (e.getStatusCode().value() == 404) {
            return new ServiceException(ErrorCode.PORTONE_PAYMENT_NOT_FOUND);
        }
        if (e.getStatusCode().value() == 409) {
            if ("PAYMENT_NOT_PAID".equalsIgnoreCase(type)) {
                return new ServiceException(ErrorCode.INVALID_PAYMENT_STATUS);
            }
            if ("PAYMENT_ALREADY_CANCELLED".equalsIgnoreCase(type)) {
                return new ServiceException(ErrorCode.INVALID_REFUND_STATUS);
            }
            return new ServiceException(ErrorCode.REFUND_FAILED);
        }
        if (e.getStatusCode().value() == 502) {
            return new ServiceException(ErrorCode.PORTONE_SERVER_ERROR);
        } // 기타 default 에러들
        return new ServiceException(ErrorCode.PORTONE_UNKNOWN_ERROR);
    }

    private PortOneErrorResponse parseErrorBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, PortOneErrorResponse.class);
        } catch (Exception e) {
            log.warn("PortOne 에러 바디 파싱 실패. responseBody : {}", responseBody, e);
            return null;
        }
    }
}

