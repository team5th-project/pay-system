package com.bootcamp.paymentdemo.refund.scheduler;

import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRetryScheduler {
    private final RefundRepository refundRepository;
    private final RefundService refundService;

    // 실패 처리 시 실패 상태, 사유 저장 , 재시도횟수 카운트 또는 초기화
    @Scheduled(fixedDelay = 60000) // 이전 작업이 끝난 후 60초 뒤에 실행
    @Transactional
    public void retryFailedRefunds() {
        List<Refund> failedRefunds = refundRepository.findByRefundStatus(RefundStatus.FAILED);

        for (Refund refund : failedRefunds) {
            try {
                refundService.retryFailedRefund(refund.getId());
            } catch (Exception e) {
                log.error("환불 재시도 중 오류 발생 refundId={}", refund.getId(), e);
            }
        }
    }
}
