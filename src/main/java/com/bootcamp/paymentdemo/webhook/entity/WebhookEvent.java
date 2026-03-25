package com.bootcamp.paymentdemo.webhook.entity;

import com.bootcamp.paymentdemo.common.BaseEntity;
import com.bootcamp.paymentdemo.common.exception.ErrorCode;
import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.webhook.enums.PortOneEventType;
import com.bootcamp.paymentdemo.webhook.enums.WebhookStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "webhook_events", // 테이블명에 Camel Case 써있길래 바꿨습니다.
        // webhookId에 DB 자체에서도 유니크 제약조건을 걸어주는 코드입니다.
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_event_webhook_id", columnNames = "webhook_id"))
public class WebhookEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB에서 사용하는 내부 PK

    // 포트원이 보낸 웹훅 이벤트 자체의 외부 고유 ID(중복 수신 방지 및 멱등성 체크용)
    @Column(nullable = false, unique = true, length = 100)
    private String webhookId;

    // 포트원이 보낸 결제 외부 식별자 (환불 식별은 paymentId 기준으로 처리한다)
    @Column(nullable = false)
    private String paymentUid;

    // 포트원이 전달한 웹훅 이벤트 타입 (Transaction.paid)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortOneEventType eventType;

    // 포트원이 보낸 웹훅 원본 JSON
    @Lob
    private String rawPayload;

    // 우리 서버 웹훅 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus webhookStatus;

    // 웹훅 처리 실패 원인 기록용
    @Column(length = 255,columnDefinition = "TEXT")
    private String failureReason;

    // 웹훅 수신시각
    private LocalDateTime receivedAt;

    // 웹훅 처리완료 시각
    private LocalDateTime processedAt;

    @Builder
    private WebhookEvent(
            String webhookId,
            String paymentUid,
            PortOneEventType eventType,
            String rawPayload,
            WebhookStatus webhookstatus,
            LocalDateTime receivedAt
    ) {
        this.webhookId = webhookId;
        this.paymentUid = paymentUid;
        this.eventType = eventType;
        this.rawPayload = rawPayload;
        this.webhookStatus = webhookstatus;
        this.receivedAt = receivedAt;
    }

    public static WebhookEvent createReceive(
            String webhookId,
            String paymentUid,
            PortOneEventType eventType,
            String rawPayload
    ) {
        return WebhookEvent.builder()
                .webhookId(webhookId)
                .paymentUid(paymentUid)
                .eventType(eventType)
                .rawPayload(rawPayload)
                .webhookstatus(WebhookStatus.RECEIVED) // 생성과 동시에 RECEIVED 상태기본값 지정
                .receivedAt(LocalDateTime.now()) // 생성과 동시에 수신시각 기록
                .build();
    }

    // 웹훅이 성공적으로 처리되었을 때 상태전이
    public void markProcessed() {
        if (this.webhookStatus != WebhookStatus.RECEIVED){
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_STATUS);
        }
        this.webhookStatus = WebhookStatus.PROCESSED;
        this.processedAt = LocalDateTime.now(); // 성공했을 때의 시각표시
        this.failureReason = null;
    }

    // 웹훅의 처리가 실패했을 때 상태전이와 동시에 실패 원인을 기록
    public void markFailed(String failureReason) {
        if (this.webhookStatus != WebhookStatus.RECEIVED) {
            throw new ServiceException(ErrorCode.INVALID_WEBHOOK_STATUS);
        }
        this.webhookStatus = WebhookStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void markIgnored(String reason) {
        this.webhookStatus = WebhookStatus.IGNORED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }
}


