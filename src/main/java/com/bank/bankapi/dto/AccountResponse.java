package com.bank.bankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountResponse {

    private UUID id;

    private UUID userId;

    private String username;

    private BigDecimal balance;

}

