package com.bank.bankapi.kafka.consumer;

import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.notification.NotificationService;
import com.bank.bankapi.notification.TransferNotification;
import com.bank.bankapi.metrics.KafkaMetrics;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TransferCompletedConsumer {

    private final NotificationService notificationService;
    private final KafkaMetrics kafkaMetrics;

    @Observed(name = "bank.kafka.consume")
    @KafkaListener(
            topics = "transfer-completed",
            groupId = "notification-group"
    )
    public void consume(TransferCompletedEvent event){
        log.info(
                "Received TransferCompletedEvent {}", event.fromAccountId()
        );
        kafkaMetrics.incrementConsumed();

        TransferNotification notification = new TransferNotification(
                event.eventId(), event.fromAccountId(), event.toAccountId(),
                event.amount(),event.occurredAt()
        );

        notificationService.sendTransferCompletedNotification(notification);
    }
}
