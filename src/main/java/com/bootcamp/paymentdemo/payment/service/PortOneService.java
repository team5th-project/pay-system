package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.common.config.PortOneProperties;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.payment.dto.response.PortOnePaymentDto;
import com.bootcamp.paymentdemo.payment.dto.response.PortOneResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortOneService {
    private final RestClient portOneRestClient;

    // TODO :
    public  void cancelPayment(String paymentUid) {

    }

    public PortOnePaymentDto getPayment(String paymentUid) {
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
                                    throw new ServiceException(ErrorCode.INVALID_PAYMENT_VALIDATION_REQUEST);
                                } else if (errorcode == 401 || errorcode == 403) { // 인증 또는 권한 문제 (Secret Key)
                                    throw new ServiceException(ErrorCode.UNAUTHORIZED_PAYMENT_VALIDATION_REQUEST);
                                } else if (errorcode == 404) {  // 1. 클라이언트 조작 가능성 2. PG -> 포트원 동기화 지연 가능성
                                    throw new ServiceException(ErrorCode.PORTONE_PAYMENT_NOT_FOUND);
                                } else {
                                    throw new ServiceException(ErrorCode.PORTONE_UNKNOWN_ERROR);
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

        } catch (ServiceException e) {  // onStatus 통해서 던진 예외 -> PaymentService에서 처리
            throw e;
        } catch (Exception e) { // 네트워크 및 기타 예외
            log.error("PortOne 통신 실패", e);
            throw new ServiceException(ErrorCode.PORTONE_COMMUNICATION_ERROR);
        }
    }
}
