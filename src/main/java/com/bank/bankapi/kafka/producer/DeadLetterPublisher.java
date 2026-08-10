package com.bank.bankapi.kafka.producer;

import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterPublisher {

    @Value("${bank.kafka.topics.transfer-completed-dlt}")
    private String topic;

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    @Observed(name = "bank.kafka.publish.dlq")
    public void publish(TransferCompletedEvent event) {

        log.warn("Publishing TransferCompletedEvent to Dead Letter Topic for sender {}",
                event.fromAccountId());

        kafkaTemplate.send(topic, event.fromAccountId().toString(), event).join();


    }
}
