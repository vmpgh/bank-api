package com.bank.bankapi.dto;

import com.bank.bankapi.transaction.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;

    private UUID accountId;

    private TransactionType type;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    /** The other account of a transfer; {@code null} for deposits and withdrawals. */
    private UUID counterpartyAccountId;

    /** Shared by both legs of a transfer; {@code null} for deposits and withdrawals. */
    private UUID transferId;

    private Instant createdAt;
}
