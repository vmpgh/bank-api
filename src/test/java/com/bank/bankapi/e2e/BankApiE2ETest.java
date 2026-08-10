package com.bank.bankapi.e2e;

import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.auth.LoginRequest;
import com.bank.bankapi.dto.auth.LoginResponse;
import com.bank.bankapi.entity.User;
import com.bank.bankapi.integration.BaseIntegrationTest;
import com.bank.bankapi.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class BankApiE2ETest extends BaseIntegrationTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build();
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminCanCreateAndRetrieveAccount() throws Exception {

        LoginRequest loginRequest =
                new LoginRequest(
                        "admin",
                        "password"
                );


        String loginResponse =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(loginRequest)
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();


        LoginResponse login =
                objectMapper.readValue(
                        loginResponse,
                        LoginResponse.class
                );


        assertThat(login.getToken())
                .isNotBlank();


        User admin =
                userRepository.findByUsername("admin")
                        .orElseThrow();


        CreateAccountRequest createRequest =
                new CreateAccountRequest();

        createRequest.setUserId(admin.getId());

        createRequest.setBalance(
                new BigDecimal("1000.00")
        );


        String accountJson =
                mockMvc.perform(
                                post("/api/v1/accounts")
                                        .header(
                                                "Authorization",
                                                "Bearer " + login.getToken()
                                        )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(createRequest)
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();


        AccountResponse createdAccount =
                objectMapper.readValue(
                        accountJson,
                        AccountResponse.class
                );


        assertThat(createdAccount.getId())
                .isNotNull();


        mockMvc.perform(
                        get("/api/v1/accounts/{id}",
                                createdAccount.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + login.getToken()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    }

}