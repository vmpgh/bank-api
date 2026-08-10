package com.bank.bankapi.unit;


import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.outbox.OutboxEvent;
import com.bank.bankapi.outbox.OutboxRepository;
import com.bank.bankapi.outbox.OutboxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class OutboxServiceImplTest {


    @Mock
    private OutboxRepository repository;

    private ObjectMapper objectMapper;


    private OutboxServiceImpl outboxService;


    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();

        outboxService =
                new OutboxServiceImpl(
                        objectMapper,
                        repository
                );
    }


    private Object getField(
            Object target,
            String fieldName
    ) throws Exception {

        Field field =
                target.getClass()
                        .getDeclaredField(fieldName);


        field.setAccessible(true);


        return field.get(target);
    }


    @Nested
    class SuccessfulSave {


        @Test
        void savesOutboxEvent() throws Exception {


            TransferCompletedEvent event =
                    new TransferCompletedEvent(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            new BigDecimal("250.00"),
                            Instant.now()
                    );


            outboxService.save(
                    "TransferCompleted",
                    event
            );


            ArgumentCaptor<OutboxEvent> captor =
                    ArgumentCaptor.forClass(
                            OutboxEvent.class
                    );


            verify(repository)
                    .save(captor.capture());


            OutboxEvent savedEvent =
                    captor.getValue();


            assertThat(
                    getField(savedEvent, "eventType")
            )
                    .isEqualTo("TransferCompleted");


            assertThat(
                    getField(savedEvent, "status")
            )
                    .hasToString("PENDING");


            assertThat(
                    getField(savedEvent, "createdAt")
            )
                    .isNotNull();


            assertThat(
                    getField(savedEvent, "nextRetryAt")
            )
                    .isNotNull();
        }
    }

    @Nested
    class EventSerialization {


        @Test
        void storesCorrectJsonPayload() throws Exception {


            UUID eventId =
                    UUID.randomUUID();

            UUID senderId =
                    UUID.randomUUID();

            UUID receiverId =
                    UUID.randomUUID();


            TransferCompletedEvent event =
                    new TransferCompletedEvent(
                            eventId,
                            senderId,
                            receiverId,
                            new BigDecimal("250.00"),
                            Instant.now()
                    );


            outboxService.save(
                    "TransferCompleted",
                    event
            );


            ArgumentCaptor<OutboxEvent> captor =
                    ArgumentCaptor.forClass(
                            OutboxEvent.class
                    );


            verify(repository)
                    .save(captor.capture());


            OutboxEvent savedEvent =
                    captor.getValue();


            String payload =
                    (String) getField(
                            savedEvent,
                            "payload"
                    );


            JsonNode json =
                    objectMapper.readTree(payload);


            assertThat(json.get("eventId").asText())
                    .isEqualTo(eventId.toString());


            assertThat(json.get("fromAccountId").asText())
                    .isEqualTo(senderId.toString());


            assertThat(json.get("toAccountId").asText())
                    .isEqualTo(receiverId.toString());


            assertThat(json.get("amount").decimalValue())
                    .isEqualByComparingTo("250.00");


            assertThat(json.get("occurredAt"))
                    .isNotNull();
        }
    }
}

