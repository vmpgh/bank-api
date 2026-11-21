package com.bank.bankapi.integration.transaction;

import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.DepositRequest;
import com.bank.bankapi.dto.TransferRequest;
import com.bank.bankapi.dto.WithdrawRequest;
import com.bank.bankapi.integration.BaseIntegrationTest;
import com.bank.bankapi.kafka.producer.TransferEventPublisher;
import com.bank.bankapi.transaction.AccountTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionHistoryControllerTest extends BaseIntegrationTest {

    private static final String HISTORY_URL = "/api/v1/accounts/{id}/transactions";

    @Autowired
    private AccountTransactionRepository transactionRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Keeps the outbox scheduler from talking to Kafka during these tests.
    @MockitoBean
    private TransferEventPublisher transferEventPublisher;

    private UUID johnAccountId;

    private UUID aliceAccountId;

    @BeforeEach
    void createAccounts() throws Exception {
        johnAccountId = createAccount("john", "1000");
        aliceAccountId = createAccount("alice", "100");
    }

    @AfterEach
    void clearRedis() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    // ===========================================================
    // ACCESS CONTROL
    // ===========================================================

    @Nested
    class Access {

        @Test
        void adminCanViewAnyAccountHistory() throws Exception {
            deposit(johnAccountId, "50");

            history(johnAccountId, admin())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void ownerCanViewOwnHistory() throws Exception {
            deposit(johnAccountId, "50");

            history(johnAccountId, john())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void nonOwnerIsForbidden() throws Exception {
            history(johnAccountId, alice())
                    .andExpect(status().isForbidden());
        }

        @Test
        void unknownAccountReturns404() throws Exception {
            history(UUID.randomUUID(), admin())
                    .andExpect(status().isNotFound());
        }

        @Test
        void requestWithoutTokenIsRejected() throws Exception {
            mockMvc.perform(get(HISTORY_URL, johnAccountId))
                    .andExpect(status().isForbidden());
        }
    }

    // ===========================================================
    // CONTENT
    // ===========================================================

    @Nested
    class Content {

        @Test
        void newAccountHasEmptyHistory() throws Exception {
            history(johnAccountId, john())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void listsDepositWithdrawalAndTransferNewestFirst() throws Exception {
            deposit(johnAccountId, "200");    // 1000 -> 1200
            withdraw(johnAccountId, "300");   // 1200 ->  900
            transfer(johnAccountId, aliceAccountId, "400"); // 900 -> 500

            history(johnAccountId, john())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(3))

                    .andExpect(jsonPath("$.content[0].type").value("TRANSFER_OUT"))
                    .andExpect(jsonPath("$.content[0].amount").value(400))
                    .andExpect(jsonPath("$.content[0].balanceAfter").value(500))
                    .andExpect(jsonPath("$.content[0].counterpartyAccountId").value(aliceAccountId.toString()))

                    .andExpect(jsonPath("$.content[1].type").value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.content[1].balanceAfter").value(900))

                    .andExpect(jsonPath("$.content[2].type").value("DEPOSIT"))
                    .andExpect(jsonPath("$.content[2].balanceAfter").value(1200))
                    .andExpect(jsonPath("$.content[2].counterpartyAccountId").value(nullValue()))
                    .andExpect(jsonPath("$.content[2].transferId").value(nullValue()));
        }

        @Test
        void transferAppearsOnBothAccountsWithSameTransferId() throws Exception {
            transfer(johnAccountId, aliceAccountId, "400");

            String johnTransferId = transferIdOfLatest(johnAccountId);

            history(aliceAccountId, alice())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].type").value("TRANSFER_IN"))
                    .andExpect(jsonPath("$.content[0].accountId").value(aliceAccountId.toString()))
                    .andExpect(jsonPath("$.content[0].counterpartyAccountId").value(johnAccountId.toString()))
                    .andExpect(jsonPath("$.content[0].balanceAfter").value(500))
                    .andExpect(jsonPath("$.content[0].transferId").value(johnTransferId));
        }

        @Test
        void failedWithdrawalIsNotRecorded() throws Exception {
            mockMvc.perform(post("/api/v1/accounts/{id}/withdraw", johnAccountId)
                            .with(john())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(withdrawRequest("5000"))))
                    .andExpect(status().isConflict());

            assertThat(transactionRepository.count()).isZero();
        }
    }

    // ===========================================================
    // PAGINATION AND FILTERS
    // ===========================================================

    @Nested
    class PaginationAndFilters {

        @Test
        void paginatesResults() throws Exception {
            deposit(johnAccountId, "10");
            deposit(johnAccountId, "20");
            deposit(johnAccountId, "30");

            history(johnAccountId, john(), "page", "1", "size", "2")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].amount").value(10))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        void filtersByType() throws Exception {
            deposit(johnAccountId, "200");
            withdraw(johnAccountId, "50");

            history(johnAccountId, john(), "type", "WITHDRAWAL")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].type").value("WITHDRAWAL"));
        }

        @Test
        void filtersByDateRange() throws Exception {
            deposit(johnAccountId, "10");
            Instant between = Instant.now();
            deposit(johnAccountId, "20");

            history(johnAccountId, john(), "from", between.toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].amount").value(20));

            history(johnAccountId, john(), "to", between.toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].amount").value(10));
        }
    }

    // ===========================================================
    // VALIDATION
    // ===========================================================

    @Nested
    class Validation {

        @ParameterizedTest(name = "{0}={1} is rejected")
        @CsvSource({
                "size, 0",
                "size, 101",
                "page, -1",
                "type, FOO",
                "from, not-a-date"
        })
        void rejectsInvalidQueryParameters(String param, String value) throws Exception {
            history(johnAccountId, john(), param, value)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void rejectsFromAfterTo() throws Exception {
            Instant now = Instant.now();

            history(johnAccountId, john(),
                    "from", now.toString(),
                    "to", now.minusSeconds(60).toString())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("'from' must not be after 'to'"));
        }

        @Test
        void rejectsMalformedAccountId() throws Exception {
            mockMvc.perform(get(HISTORY_URL, "not-a-uuid").with(admin()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
        }
    }

    // ===========================================================
    // HELPERS
    // ===========================================================

    /** GET history; {@code params} are name/value pairs. */
    private ResultActions history(UUID accountId, RequestPostProcessor user, String... params) throws Exception {
        var request = get(HISTORY_URL, accountId).with(user);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    private String transferIdOfLatest(UUID accountId) throws Exception {
        String json = history(accountId, admin())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("content").get(0).get("transferId").asString();
    }

    private UUID createAccount(String username, String balance) throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setUserId(user(username).getId());
        request.setBalance(new BigDecimal(balance));

        String json = mockMvc.perform(post("/api/v1/accounts")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(json, AccountResponse.class).getId();
    }

    private void deposit(UUID accountId, String amount) throws Exception {
        DepositRequest request = new DepositRequest();
        request.setDepositAmount(new BigDecimal(amount));

        mockMvc.perform(post("/api/v1/accounts/{id}/deposit", accountId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private void withdraw(UUID accountId, String amount) throws Exception {
        mockMvc.perform(post("/api/v1/accounts/{id}/withdraw", accountId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest(amount))))
                .andExpect(status().isOk());
    }

    private WithdrawRequest withdrawRequest(String amount) {
        WithdrawRequest request = new WithdrawRequest();
        request.setWithdrawAmount(new BigDecimal(amount));
        return request;
    }

    private void transfer(UUID from, UUID to, String amount) throws Exception {
        TransferRequest request = new TransferRequest(from, to, new BigDecimal(amount));

        mockMvc.perform(post("/api/v1/transfers")
                        .with(admin())
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
