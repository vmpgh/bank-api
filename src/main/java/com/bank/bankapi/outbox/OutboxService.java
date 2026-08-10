package com.bank.bankapi.outbox;

public interface OutboxService {

    void save(String eventType, Object event);
}
