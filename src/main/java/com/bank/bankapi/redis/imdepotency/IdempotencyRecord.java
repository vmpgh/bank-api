package com.bank.bankapi.redis.imdepotency;

public record IdempotencyRecord(
        String status,
        int httpStatus
) {
}
