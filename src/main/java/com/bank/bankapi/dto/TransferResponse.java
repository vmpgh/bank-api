package com.bank.bankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class TransferResponse {

    private UUID senderAccountId;

    private UUID receiverAccountId;

    private BigDecimal transferredAmount;

    private BigDecimal senderBalance;

    private BigDecimal receiverBalance;

    private Instant completedAt;
}