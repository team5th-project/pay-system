package com.bootcamp.paymentdemo.refund.scheduler;

import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.refund.enums.RefundStatus;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRetryScheduler {
    private final RefundRepository refundRepository;
    private final RefundService refundService;

    // 실패 처리 시 실패 상태, 사유 저장 , 재시도횟수 카운트 또는 초기화
    @Scheduled(fixedDelay = 60000) // 이전 작업이 끝난 후 60초 뒤에 실행
    public void retryRefunds() {

        LocalDateTime now = LocalDateTime.now();
        // 스케줄러가 지금 재시도해야 할 실패목록 조회
        List<Refund> targets = refundRepository.findRetryableTargets(
                RefundStatus.FAILED_RETRYABLE, now);

        if (targets.isEmpty()) {
            log.debug("재시도 대상 환불 없음");
            return;
        }
        log.info("환불 재시도 스케줄러 시작 targetCount={}", targets.size());

        // 하나씩 찾아보기
        for (Refund refund : targets) {
            try {
                refundService.retryRefund(refund.getId());
            } catch (Exception e) {
                // 한 건 실패해도 다음 건은 계속 처리 진행
                log.error("환불 재시도 스케줄러 처리 실패 refundId={}", refund.getId(), e);
            }
        }
    }
}
