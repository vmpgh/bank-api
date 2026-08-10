package com.bank.bankapi.kafka.producer;

import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.metrics.KafkaMetrics;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventPublisher {

    private final KafkaMetrics kafkaMetrics;

    @Value("${bank.kafka.topics.transfer-completed}")
    private String topic;

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    @Observed(name = "bank.kafka.publish")
    public void publish(TransferCompletedEvent event){

        log.info("Publishing TransferCompletedEvent event {} for sender {} to {}" ,
                event.eventId(), event.fromAccountId(), topic);

        kafkaTemplate.send(topic, event.fromAccountId().toString(),event).join();
        kafkaMetrics.incrementPublished();
    }


}
