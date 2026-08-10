package com.bank.bankapi.outbox;


import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int retryCount = 0;

    private Instant nextRetryAt;

    private String lastError;

    private Instant createdAt;

    private Instant publishedAt;

    private static final long BASE_RETRY_DELAY_SECONDS = 30L;


    protected OutboxEvent() {
    }

    public OutboxEvent(String eventType,
                       String payload) {

        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.payload = payload;
        this.nextRetryAt = Instant.now();
        this.createdAt = Instant.now();
        this.status = OutboxStatus.PENDING;
    }

    public int markForRetry(String error) {

        retryCount++;

        lastError = error;

        long delaySeconds =
                BASE_RETRY_DELAY_SECONDS *
                        (long) Math.pow(2, retryCount - 1);

        nextRetryAt =
                Instant.now().plusSeconds(delaySeconds);

        return retryCount;
    }


    public void markPublished() {
        status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        nextRetryAt = null;
        lastError = null;
    }

    public void markFailed() {
        status = OutboxStatus.FAILED;
        nextRetryAt = null;
    }

    public boolean shouldMoveToDeadLetter(int maxRetries) {
        return retryCount >= maxRetries;
    }
}
