package com.bank.bankapi.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent>
    findTop100ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
            OutboxStatus status, Instant now
    );
}
