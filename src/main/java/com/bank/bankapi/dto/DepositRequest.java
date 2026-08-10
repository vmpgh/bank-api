package com.bank.bankapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class DepositRequest {

    @NotNull(message = "Deposit amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal depositAmount;


}
