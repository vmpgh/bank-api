package com.bank.bankapi.outbox;

public enum OutboxStatus {

    PENDING,
    PUBLISHED,
    FAILED
}
