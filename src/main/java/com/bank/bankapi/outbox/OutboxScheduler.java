package com.bank.bankapi.outbox;

import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.kafka.producer.DeadLetterPublisher;
import com.bank.bankapi.kafka.producer.TransferEventPublisher;
import com.bank.bankapi.metrics.KafkaMetrics;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutboxScheduler {


    private static final int MAX_RETRIES = 5;
    private final OutboxRepository repository;
    private final TransferEventPublisher transferEventPublisher;
    private final ObjectMapper objectMapper;
    private final DeadLetterPublisher deadLetterPublisher;
    private final KafkaMetrics kafkaMetrics;

    @Scheduled(fixedDelay = 5000)
    @Observed(name = "bank.outbox.publish")
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                repository.findTop100ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
                        OutboxStatus.PENDING,
                        Instant.now()
                );

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} pending events", events.size());

        for (OutboxEvent outbox : events) {

            TransferCompletedEvent event;

            // Phase 1 - Deserialize
            try {

                event = objectMapper.readValue(
                        outbox.getPayload(),
                        TransferCompletedEvent.class
                );

            } catch (Exception e) {

                log.error(
                        "Failed to deserialize Outbox event {}",
                        outbox.getId(),
                        e
                );

                outbox.markFailed();
                repository.save(outbox);

                continue;
            }

            // Phase 2 - Already exhausted retries?
            if (outbox.shouldMoveToDeadLetter(MAX_RETRIES)) {

                try {

                    deadLetterPublisher.publish(event);
                    kafkaMetrics.incrementDeadLetterEvents();
                    outbox.markFailed();

                    repository.save(outbox);

                    log.error(
                            "Outbox event {} moved to Dead Letter Topic",
                            outbox.getId()
                    );

                } catch (Exception dlqException) {

                    log.error(
                            "Failed to publish Outbox event {} to Dead Letter Topic",
                            outbox.getId(),
                            dlqException
                    );

                    // Keep it pending so we'll try the DLQ again later.
                    outbox.markForRetry(dlqException.getMessage());
                    kafkaMetrics.incrementOutboxRetries();

                    repository.save(outbox);
                }

                continue;
            }

            // Phase 3 - Normal publish
            try {

                transferEventPublisher.publish(event);

                outbox.markPublished();

                repository.save(outbox);

                log.info(
                        "Published Outbox event {}",
                        outbox.getId()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to publish Outbox event {}",
                        outbox.getId(),
                        e
                );

                outbox.markForRetry(e.getMessage());
                kafkaMetrics.incrementOutboxRetries();

                repository.save(outbox);
            }
        }
    }
}