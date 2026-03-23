package com.bootcamp.paymentdemo.order.scheduler;

import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.order.enums.OrderStatus;
import com.bootcamp.paymentdemo.order.repository.OrderRepository;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.service.PaymentService;
import com.bootcamp.paymentdemo.point.service.UserPointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


//  주문 자동 확정 스케줄러
//
//  - 결제 완료(PAID) 상태의 주문 중 7일이 경과한 주문을 매일 자정에 자동으로 CONFIRMED 전환
//  - 환불 가능 기간(7일) 이 지난 주문은 사용자가 확정 버튼을 누르지 않아도 자동 확정

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final UserPointService userPointService;

//
//      7일 경과 PAID 주문 자동 확정
//
//      - 매일 자정(00:00:00) 실행
//      - PAID 상태이면서 createdAt 기준 7일이 지난 주문 전체 조회
//      - 각 주문을 CONFIRMED 로 상태 전이 후 포인트 적립
//      - 처리 중 개별 주문에서 예외 발생 시 해당 주문만 스킵하고 나머지 계속 처리
//        -> 한 건 실패가 전체 배치에 영향 주지 않도록 방어
//
    @Scheduled(cron = "0 0 0 * * *")  // 매일 자정 실행
    @Transactional
    public void autoConfirmOrders() {
        // 현재 시각 기준 7일 전 시점 계산
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // PAID 상태이면서 7일 경과한 주문 목록 조회
        List<Order> targets = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PAID, sevenDaysAgo
        );

        if (targets.isEmpty()) {
            log.info("[OrderScheduler] 자동 확정 대상 주문 없음");
            return;
        }

        log.info("[OrderScheduler] 자동 확정 대상 주문 수: {}", targets.size());

        for (Order order : targets) {
            try {
                // 1. 주문 상태 PAID → CONFIRMED 전환
                order.confirm();

                // 2. 포인트 적립 (실제 PG 결제 금액 기준)
                // finalAmount = totalAmount - pointToUse (포인트 차감 후 실제 결제 금액)
                Payment payment = paymentService.getPaymentByOrderId(order.getId());
                userPointService.earnPoint(order.getUserId(), order.getId(), payment.getFinalAmount());

                log.info("[OrderScheduler] 주문 자동 확정 완료 - orderId: {}", order.getId());

            } catch (Exception e) {
                // 개별 주문 처리 실패 시 로그만 남기고 다음 주문 처리 계속 진행
                log.error("[OrderScheduler] 주문 자동 확정 실패 - orderId: {}, error: {}",
                        order.getId(), e.getMessage(), e);
            }
        }
    }
}
