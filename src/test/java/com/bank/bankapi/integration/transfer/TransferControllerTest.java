package com.bank.bankapi.integration.transfer;

import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.TransferRequest;
import com.bank.bankapi.entity.User;
import com.bank.bankapi.integration.BaseIntegrationTest;
import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.kafka.producer.TransferEventPublisher;
import com.bank.bankapi.outbox.OutboxEvent;
import com.bank.bankapi.outbox.OutboxRepository;
import com.bank.bankapi.outbox.OutboxScheduler;
import com.bank.bankapi.outbox.OutboxStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferControllerTest extends BaseIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private TransferEventPublisher transferEventPublisher;

    @Autowired
    private OutboxScheduler outboxScheduler;


    @AfterEach
    void clearRedis() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }


    @Nested
    class Transfer {

        @Test
        void adminCanTransfer() throws Exception {

            User john = user("john");
            User alice = user("alice");

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            // Create Alice's account

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(200)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(admin())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", johnAccount.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(300));

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", aliceAccount.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(300));
        }

        @Test
        void ownerCanTransfer() throws Exception {

            User john = user("john");
            User alice = user("alice");

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            // Create Alice's account

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(200)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", johnAccount.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(300));

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", aliceAccount.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(300));

        }

        @Test
        void cannotTransferToYourself() throws Exception {

            User john = user("john");

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));


            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);
            assertNotNull(johnAccount.getId());

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            johnAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Sender and receiver accounts must be different."));

        }

        @Test
        void returns404WhenSenderDoesNotExist() throws Exception {

            User alice = user("alice");

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            UUID.randomUUID(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(admin())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns404WhenReceiverDoesNotExist() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(500));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            UUID.randomUUID(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(admin())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void rejectsInsufficientFunds() throws Exception {

            User john = user("john");
            User alice = user("alice");

            // Create John's account

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(500)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(admin())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message")
                            .value("Account " + johnAccount.getId() + " has insufficient funds."));
        }

        @Test
        void aliceCannotTransferFromJohnsAccount() throws Exception {

            User john = user("john");
            User alice = user("alice");

            // Create John's account

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(alice())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isForbidden());

        }
    }

    @Nested
    class Idempotency {

        @Test
        void duplicateTransferIsProcessedOnlyOnce() throws Exception {

            User john = user("john");
            User alice = user("alice");

            // Create John's account

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            String key = UUID.randomUUID().toString();

            // First request

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            // Verify sender balance

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", johnAccount.getId())
                                    .with(john())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(400));

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", aliceAccount.getId())
                                    .with(alice())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(200));


        }

        @Test
        void differentKeyCreatesNewTransfer() throws Exception {

            User john = user("john");
            User alice = user("alice");

            // Create John's account

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            // First transfer

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            // Verify John's balance

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", johnAccount.getId())
                                    .with(john())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(300));

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", aliceAccount.getId())
                                    .with(alice())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(300));
        }

        @Test
        void failedTransferReleasesIdempotencyKey() throws Exception {

            User john = user("john");
            User alice = user("alice");

            // Create John's account

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            String key = UUID.randomUUID().toString();

            // First transfer (fails)

            TransferRequest failedTransfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(1000)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(failedTransfer))
                    )
                    .andExpect(status().isConflict());


            TransferRequest successfulTransfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", key)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(successfulTransfer))
                    )
                    .andExpect(status().isOk());

            // Verify balances

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", johnAccount.getId())
                                    .with(john())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(400));

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", aliceAccount.getId())
                                    .with(alice())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(200));
        }

    }


    @Nested
    class RateLimiting {

        @Test
        void rejectsWhenRateLimitExceeded() throws Exception {

            // Make sure previous tests don't affect this one
            redisTemplate.delete("rate_limit:john");

            User john = user("john");
            User alice = user("alice");

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(1000));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);


            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.TEN
                    );

            // First 5 requests should succeed

            for (int i = 0; i < 5; i++) {

                mockMvc.perform(
                                post("/api/v1/transfers")
                                        .with(john())
                                        .header("Idempotency-Key", UUID.randomUUID().toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(transfer))
                        )
                        .andExpect(status().isOk());

            }

            // 6th request should be blocked

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.message")
                            .value("Rate limit exceeded."));
        }


    }

    @Nested
    class Outbox {

        @Test
        void createsOutboxEvent() throws Exception {

            User john = user("john");
            User alice = user("alice");

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            // Verify Outbox

            List<OutboxEvent> events = outboxRepository.findAll();

            assertThat(events).hasSize(1);

            OutboxEvent event = events.getFirst();

            assertThat(event.getEventType())
                    .isEqualTo("TransferCompleted");

            assertThat(event.getStatus())
                    .isEqualTo(OutboxStatus.PENDING);

            assertThat(event.getPayload())
                    .isNotBlank();

            assertThat(event.getCreatedAt())
                    .isNotNull();
        }

        @Test
        void schedulerPublishesPendingEvents() throws Exception {

            User john = user("john");
            User alice = user("alice");

            CreateAccountRequest johnRequest = new CreateAccountRequest();
            johnRequest.setUserId(john.getId());
            johnRequest.setBalance(BigDecimal.valueOf(500));

            MvcResult johnResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(johnRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse johnAccount =
                    objectMapper.readValue(
                            johnResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            CreateAccountRequest aliceRequest = new CreateAccountRequest();
            aliceRequest.setUserId(alice.getId());
            aliceRequest.setBalance(BigDecimal.valueOf(100));

            MvcResult aliceResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(aliceRequest))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse aliceAccount =
                    objectMapper.readValue(
                            aliceResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            TransferRequest transfer =
                    new TransferRequest(
                            johnAccount.getId(),
                            aliceAccount.getId(),
                            BigDecimal.valueOf(100)
                    );

            mockMvc.perform(
                            post("/api/v1/transfers")
                                    .with(john())
                                    .header("Idempotency-Key", UUID.randomUUID().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(transfer))
                    )
                    .andExpect(status().isOk());

            // Execute scheduler manually

            outboxScheduler.publishPendingEvents();

            // Verify publisher called

            verify(transferEventPublisher, times(1))
                    .publish(any(TransferCompletedEvent.class));

            // Verify Outbox status updated

            OutboxEvent event =
                    outboxRepository.findAll()
                            .stream()
                            .max(Comparator.comparing(OutboxEvent::getCreatedAt))
                            .orElseThrow();

            assertThat(event.getStatus())
                    .isEqualTo(OutboxStatus.PUBLISHED);

            assertThat(event.getPublishedAt())
                    .isNotNull();
        }
    }
}
