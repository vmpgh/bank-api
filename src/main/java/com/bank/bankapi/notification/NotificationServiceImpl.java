package com.bank.bankapi.notification;

import com.bank.bankapi.metrics.KafkaMetrics;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServiceImpl implements NotificationService {

    private final ProcessedEventRepository processedEventRepository;
    private final KafkaMetrics kafkaMetrics;

    @Observed(name = "bank.notification.send")
    @Transactional
    @Override
    public void sendTransferCompletedNotification(TransferNotification notification) {

        if(processedEventRepository.existsById(notification.eventId())){

            log.warn(" Duplicate TransferNotification with eventId {} detected. Ignoring.", notification.eventId());
            kafkaMetrics.incrementDuplicateNotifications();
            return;
        }

        log.info("""
                
                Notification:
                Transfer {} completed.
                From: {}
                To: {}
                Amount: {}
                Completed at: {}
                """,
                notification.eventId(),
                notification.senderAccountId(),
                notification.receiverAccountId(),
                notification.amount(),
                notification.completedAt()
        );
        processedEventRepository.save(
                new ProcessedEvent(notification.eventId(), Instant.now())
        );
        kafkaMetrics.incrementNotificationsSent();
    }
}
