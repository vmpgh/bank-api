package com.bank.bankapi.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferNotification(

        UUID eventId,

        UUID senderAccountId,

        UUID receiverAccountId,

        BigDecimal amount,

        Instant completedAt
) {
}
