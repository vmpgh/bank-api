package com.bank.bankapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransferRequest {


    @NotNull(message = "Sender account ID is required")
    private UUID senderAccountId;

    @NotNull(message = "Receiver account ID is required")
    private UUID receiverAccountId;

    @NotNull
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
}
