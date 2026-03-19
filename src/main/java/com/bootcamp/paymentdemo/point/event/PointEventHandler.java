package com.bootcamp.paymentdemo.point.event;

import com.bootcamp.paymentdemo.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointEventHandler {

    private final PointService pointService;

    // 주문 확정 이벤트 수신 → 포인트 적립
    // @EventListener
    // public void handleOrderConfirmed(OrderConfirmedEvent event) {}

    // 결제 완료 이벤트 수신 → 멤버십 등급 갱신
    // @EventListener
    // public void handlePaymentCompleted(PaymentCompletedEvent event) {}

    // 환불 완료 이벤트 수신 → 등급 롤백
    // @EventListener
    // public void handleRefundCompleted(RefundCompletedEvent event) {}
}