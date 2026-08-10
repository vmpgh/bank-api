package com.bank.bankapi.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class KafkaMetrics {

    private final Counter eventsPublished;
    private final Counter eventsConsumed;
    private final Counter notificationsSent;
    private final Counter duplicateNotifications;
    private final Counter outboxRetries;
    private final Counter deadLetterEvents;

    public KafkaMetrics(MeterRegistry registry) {

        eventsPublished = Counter.builder("bank.kafka.events.published")
                .description("Kafka events published")
                .register(registry);

        eventsConsumed = Counter.builder("bank.kafka.events.consumed")
                .description("Kafka events consumed")
                .register(registry);

        notificationsSent = Counter.builder("bank.notifications.sent")
                .description("Notifications sent")
                .register(registry);

        duplicateNotifications = Counter.builder("bank.notifications.duplicates")
                .description("Duplicate notifications ignored")
                .register(registry);

        outboxRetries = Counter.builder("bank.outbox.retries")
                .description("Outbox retry attempts")
                .register(registry);

        deadLetterEvents = Counter.builder("bank.kafka.dlq")
                .description("Events sent to the Dead Letter Queue")
                .register(registry);
    }

    public void incrementPublished() {
        eventsPublished.increment();
    }

    public void incrementConsumed() {
        eventsConsumed.increment();
    }

    public void incrementNotificationsSent() {
        notificationsSent.increment();
    }

    public void incrementDuplicateNotifications() {
        duplicateNotifications.increment();
    }

    public void incrementOutboxRetries() {
        outboxRetries.increment();
    }

    public void incrementDeadLetterEvents() {
        deadLetterEvents.increment();
    }
}
