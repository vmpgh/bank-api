package com.bank.bankapi.transaction;

import com.bank.bankapi.dto.TransactionResponse;
import com.bank.bankapi.entity.Account;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.mapper.TransactionMapper;
import com.bank.bankapi.repository.AccountRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final AccountTransactionRepository transactionRepository;

    private final AccountRepository accountRepository;

    private final TransactionMapper transactionMapper;

    // Write methods use MANDATORY so a ledger entry can never be saved
    // outside the transaction that changed the balance.

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordDeposit(Account account, BigDecimal amount) {
        transactionRepository.save(AccountTransaction.deposit(account, amount, Instant.now()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordWithdrawal(Account account, BigDecimal amount) {
        transactionRepository.save(AccountTransaction.withdrawal(account, amount, Instant.now()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordTransfer(Account sender, Account receiver, BigDecimal amount,
                               UUID transferId, Instant at) {
        transactionRepository.saveAll(List.of(
                AccountTransaction.transferOut(sender, receiver.getId(), amount, transferId, at),
                AccountTransaction.transferIn(receiver, sender.getId(), amount, transferId, at)
        ));
    }

    @Override
    @Observed(name = "bank.transactions.history")
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistory(UUID accountId,
                                                TransactionHistoryFilter filter,
                                                Pageable pageable) {
        log.info("Loading transaction history for account {}", accountId);

        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account ", accountId);
        }

        Pageable newestFirst =
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);

        return transactionRepository
                .findAll(AccountTransactionSpecifications.matching(accountId, filter), newestFirst)
                .map(transactionMapper::toResponse);
    }
}
