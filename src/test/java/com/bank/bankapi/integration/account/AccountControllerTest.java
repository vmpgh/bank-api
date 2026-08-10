package com.bank.bankapi.integration.account;

import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.DepositRequest;
import com.bank.bankapi.dto.WithdrawRequest;
import com.bank.bankapi.entity.Account;
import com.bank.bankapi.entity.User;
import com.bank.bankapi.integration.BaseIntegrationTest;
import com.bank.bankapi.repository.AccountRepository;
import com.bank.bankapi.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

public class AccountControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Nested
    class CreateAccount {
        @Test
        void adminCanCreateAccount() throws Exception {

            User john = userRepository.findByUsername("john")
                    .orElseThrow();

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(1000));

            MvcResult result =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .accept(MediaType.APPLICATION_JSON)
                                            .content(
                                                    objectMapper.writeValueAsString(request)
                                            )
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse response =
                    objectMapper.readValue(
                            result.getResponse().getContentAsString(),
                            AccountResponse.class);

            assertThat(response.getUserId()).isEqualTo(john.getId());
            assertThat(response.getBalance())
                    .isEqualByComparingTo("1000");

            Account account = accountRepository.findById(response.getId())
                    .orElseThrow();

            assertThat(account.getBalance())
                    .isEqualByComparingTo("1000");

            assertThat(account.getOwner().getId())
                    .isEqualTo(john.getId());

        }

        @Test
        void userCannotCreateAccount() throws Exception {

            User john = userRepository.findByUsername("john")
                    .orElseThrow();

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

            mockMvc.perform(
                            post("/api/v1/accounts")
                                    .with(john())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        void returns404WhenUserDoesNotExist() throws Exception {

            UUID randomUser = UUID.randomUUID();

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(randomUser);
            request.setBalance(BigDecimal.valueOf(100));

            mockMvc.perform(
                            post("/api/v1/accounts")
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void rejectsNegativeBalance() throws Exception {
            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(-100));

            mockMvc.perform(
                            post("/api/v1/accounts")
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.balance")
                            .value("Balance cannot be negative"));

        }

        @Test
        void rejectsNullUserId() throws Exception {

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(null);
            request.setBalance(BigDecimal.valueOf(100));

            mockMvc.perform(
                            post("/api/v1/accounts")
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.userId")
                            .value("User Id is required"));
        }

    }


    @Nested
    class GetAccount {

        @Test
        void adminCanReadAnyAccount() throws Exception {

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

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", account.getId())
                                    .with(admin())
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(account.getId().toString()))
                    .andExpect(jsonPath("$.userId").value(john.getId().toString()))
                    .andExpect(jsonPath("$.username").value("john"))
                    .andExpect(jsonPath("$.balance").value(500));
        }

        @Test
        void ownerCanReadOwnAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(500));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", account.getId())
                                    .with(john())
                    )
                    .andExpect(status().isOk());
        }


        @Test
        void aliceCannotReadJohnAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(500));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", account.getId())
                                    .with(alice())
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        void returns404WhenAccountDoesNotExist() throws Exception {

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", UUID.randomUUID())
                                    .with(admin())
                    )
                    .andExpect(status().isNotFound());
        }

    }

    @Nested
    class Deposit {

        @Test
        void adminCanDeposit() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

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

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            DepositRequest deposit = new DepositRequest();
            deposit.setDepositAmount(BigDecimal.valueOf(100));
            System.out.println(objectMapper.writeValueAsString(deposit));
            mockMvc.perform(
                            post("/api/v1/accounts/{id}/deposit", account.getId())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(deposit))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(200));
        }

        @Test
        void ownerCanDeposit() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

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

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            DepositRequest deposit = new DepositRequest();
            deposit.setDepositAmount(BigDecimal.valueOf(100));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/deposit", account.getId())
                                    .with(john())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(deposit))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(200));
        }

        @Test
        void aliceCannotDepositIntoJohnAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            DepositRequest deposit = new DepositRequest();
            deposit.setDepositAmount(BigDecimal.valueOf(50));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/deposit", account.getId())
                                    .with(alice())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(deposit))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        void rejectsNegativeDepositAmount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            DepositRequest deposit = new DepositRequest();
            deposit.setDepositAmount(BigDecimal.valueOf(-50));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/deposit", account.getId())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(deposit))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.depositAmount")
                            .value("Amount must be greater than zero"));
        }

        @Test
        void returns404WhenDepositingToUnknownAccount() throws Exception {

            DepositRequest deposit = new DepositRequest();
            deposit.setDepositAmount(BigDecimal.valueOf(100));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/deposit", UUID.randomUUID())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(deposit))
                    )
                    .andExpect(status().isNotFound());
        }

    }

    @Nested
    class Withdraw {

        @Test
        void adminCanWithdraw() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(200));

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

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            WithdrawRequest withdraw = new WithdrawRequest();
            withdraw.setWithdrawAmount(BigDecimal.valueOf(50));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/withdraw", account.getId())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(withdraw))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(150));
        }

        @Test
        void ownerCanWithdraw() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(200));

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

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            WithdrawRequest withdraw = new WithdrawRequest();
            withdraw.setWithdrawAmount(BigDecimal.valueOf(50));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/withdraw", account.getId())
                                    .with(john())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(withdraw))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(150));


        }

        @Test
        void aliceCannotWithdrawFromJohnAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(200));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            WithdrawRequest withdraw = new WithdrawRequest();
            withdraw.setWithdrawAmount(BigDecimal.valueOf(50));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/withdraw", account.getId())
                                    .with(alice())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(withdraw))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        void rejectsNegativeWithdrawAmount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(200));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            WithdrawRequest withdraw = new WithdrawRequest();
            withdraw.setWithdrawAmount(BigDecimal.valueOf(-50));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/withdraw", account.getId())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(withdraw))
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.withdrawAmount")
                            .value("Amount must be greater than zero"));
        }

        @Test
        void rejectsInsufficientFunds() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.valueOf(100));

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            WithdrawRequest withdraw = new WithdrawRequest();
            withdraw.setWithdrawAmount(BigDecimal.valueOf(1000));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/withdraw", account.getId())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(withdraw))
                    )
                    .andExpect(status().isConflict());
        }

        @Test
        void returns404WhenWithdrawingFromUnknownAccount() throws Exception {

            WithdrawRequest withdraw = new WithdrawRequest();
            withdraw.setWithdrawAmount(BigDecimal.valueOf(100));

            mockMvc.perform(
                            post("/api/v1/accounts/{id}/withdraw", UUID.randomUUID())
                                    .with(admin())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(withdraw))
                    )
                    .andExpect(status().isNotFound());
        }





    }

    @Nested
    class DeleteAccount {

        @Test
        void adminCanDeleteAnyAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.ZERO);

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

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            mockMvc.perform(
                            delete("/api/v1/accounts/{id}", account.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isNoContent());

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", account.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void ownerCanDeleteOwnAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.ZERO);

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            mockMvc.perform(
                            delete("/api/v1/accounts/{id}", account.getId())
                                    .with(john())
                    )
                    .andExpect(status().isNoContent());

            mockMvc.perform(
                            get("/api/v1/accounts/{id}", account.getId())
                                    .with(admin())
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        void aliceCannotDeleteJohnAccount() throws Exception {

            User john = user("john");

            CreateAccountRequest request = new CreateAccountRequest();
            request.setUserId(john.getId());
            request.setBalance(BigDecimal.ZERO);

            MvcResult createResult =
                    mockMvc.perform(
                                    post("/api/v1/accounts")
                                            .with(admin())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(request))
                            )
                            .andExpect(status().isCreated())
                            .andReturn();

            AccountResponse account =
                    objectMapper.readValue(
                            createResult.getResponse().getContentAsString(),
                            AccountResponse.class);

            mockMvc.perform(
                            delete("/api/v1/accounts/{id}", account.getId())
                                    .with(alice())
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        void returns404WhenDeletingUnknownAccount() throws Exception {

            mockMvc.perform(
                            delete("/api/v1/accounts/{id}", UUID.randomUUID())
                                    .with(admin())
                    )
                    .andExpect(status().isNotFound());
        }

    }

}
