package com.bank.bankapi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transferCompletedTopic(
            @Value("${bank.kafka.topics.transfer-completed}") String topicName) {

        return TopicBuilder
                .name(topicName)
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic transferCompletedDltTopic(
            @Value("${bank.kafka.topics.transfer-completed-dlt}") String topicName) {

        return TopicBuilder
                .name(topicName)
                .partitions(3)
                .build();
    }
}