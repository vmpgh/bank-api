package com.bank.bankapi.transaction;

import java.time.Instant;

/**
 * Optional filters for transaction history. Any field may be {@code null}.
 * {@code from} is inclusive, {@code to} is exclusive.
 */
public record TransactionHistoryFilter(TransactionType type, Instant from, Instant to) {

    public TransactionHistoryFilter {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
    }

    public static TransactionHistoryFilter none() {
        return new TransactionHistoryFilter(null, null, null);
    }
}
