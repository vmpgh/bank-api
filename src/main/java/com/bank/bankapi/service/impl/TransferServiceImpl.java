package com.bank.bankapi.service.impl;

import com.bank.bankapi.dto.TransferRequest;
import com.bank.bankapi.dto.TransferResponse;
import com.bank.bankapi.entity.Account;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.kafka.event.TransferCompletedEvent;
import com.bank.bankapi.metrics.BankMetrics;
import com.bank.bankapi.repository.AccountRepository;
import com.bank.bankapi.outbox.OutboxService;
import com.bank.bankapi.service.TransferService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;

    private final BankMetrics bankMetrics;

    //private final TransferEventPublisher publisher;

    private final OutboxService outboxService;

    @Override
    @Observed(name = "bank.transfer")
    @Caching(evict = {
            @CacheEvict(value = "accounts", key = "#request.senderAccountId"),
            @CacheEvict(value = "accounts", key = "#request.receiverAccountId")
    })
    @Transactional
    public TransferResponse transfer(TransferRequest request) {

        if (request.getSenderAccountId().equals(request.getReceiverAccountId())) {
            throw new IllegalArgumentException("Sender and receiver accounts must be different.");
        }
        Account sender = accountRepository.findById(request.getSenderAccountId())
                .orElseThrow(() -> new ResourceNotFoundException
                        ("Account", request.getSenderAccountId()));

        Account receiver = accountRepository.findById(request.getReceiverAccountId())
                .orElseThrow(() -> new ResourceNotFoundException
                        ("Account", request.getReceiverAccountId()));

        sender.withdraw(request.getAmount());
        receiver.deposit(request.getAmount());
        Instant completedAt = Instant.now();
        TransferCompletedEvent event =
                new TransferCompletedEvent(
                        UUID.randomUUID(),
                        request.getSenderAccountId(),
                        request.getReceiverAccountId(),
                        request.getAmount(),
                        completedAt
                );

        outboxService.save(
                "TransferCompleted",
                event
        );
        bankMetrics.incrementTransfersMade();

        log.info("Transfer of {} from {} to {} was successful",
                request.getAmount(), request.getSenderAccountId(),
                request.getReceiverAccountId());

        return new TransferResponse(
                sender.getId(),
                receiver.getId(),
                request.getAmount(),
                sender.getBalance(),
                receiver.getBalance(),
                completedAt
        );
    }
}
