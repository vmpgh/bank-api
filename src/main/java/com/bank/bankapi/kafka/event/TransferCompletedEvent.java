package com.bank.bankapi.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(


        UUID eventId,

        UUID fromAccountId,

        UUID toAccountId,

        BigDecimal amount,

        Instant occurredAt

) {
}
