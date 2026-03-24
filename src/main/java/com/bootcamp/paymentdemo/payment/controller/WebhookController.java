//package com.bootcamp.paymentdemo.payment.controller;
//
//import com.bootcamp.paymentdemo.common.global.CommonResponse;
//import com.bootcamp.paymentdemo.payment.dto.response.WebhookResponse;
//import com.bootcamp.paymentdemo.payment.service.PaymentService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/webhooks")
//@RequiredArgsConstructor
//public class WebhookController {
//    private final PaymentService paymentService;
//
//    // 멱등성을 위해 처리된 웹훅 기록 in-memory
//    // TODO : 이거 웹훅 테이블에 저장해야 할 것 같다.
//    private final Set<String> processedWebHooks = ConcurrentHashMap.newKeySet();
//
//    @Value("${portone.webhook.secret}")
//    private String webhookSecret;
//
//    @PostMapping("/portone")
//    public ResponseEntity<CommonResponse<WebhookResponse>> handlePortOneWebhook(
//            @RequestBody Map<String,Object> rawRequest)
//    {
//        log.info("PortOne 웹훅 수신 - 데이터 : {}", rawRequest);
//
//        String paymentId = null;
//        String type = null;
//
//        // V2 스키마 확인
//        if (rawRequest.containsKey("type") && rawRequest.get("data") instanceof Map<?, ?>) {
//            type = (String) rawRequest.get("type");
//            Map<String, Object> data = (Map<String, Object>) rawRequest.get("data");
//            paymentId = (String) data.get("paymentId");
//            log.info("V2 형식 웹훅 감지 - type :{}, paymentId : {}", type, paymentId);
//
//        }
//
//
//        return null;
//    }
//
//}
