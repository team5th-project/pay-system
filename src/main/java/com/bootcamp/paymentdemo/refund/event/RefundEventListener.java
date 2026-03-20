package com.bootcamp.paymentdemo.refund.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
public class RefundEventListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)

    public void handleRefundCompleted(RefundCompletedEvent event) {
        log.info("환불 완료 이벤트 수신: refundId={}, paymentId={}, orderId={}",
                event.refundId(), event.paymentId(), event.orderId());

        // TODO: Point 도메인에서 orderId 기준 포인트 복구

        // TODO: Point 도메인에서 orderId 기준 적립 취소
    }
}