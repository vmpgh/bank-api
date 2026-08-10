package com.bank.bankapi.outbox;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final ObjectMapper objectMapper;
    private final OutboxRepository repository;


    @Override
    @Observed(name = "bank.outbox.save")
    public void save(String eventType, Object event) {
        String payload =
                objectMapper.writeValueAsString(event);

        repository.save(
                new OutboxEvent(
                        eventType,
                        payload
                )
        );


    }
}

