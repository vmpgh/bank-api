package com.bank.bankapi.service.impl;

import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.DepositRequest;
import com.bank.bankapi.dto.WithdrawRequest;
import com.bank.bankapi.entity.Account;
import com.bank.bankapi.entity.User;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.mapper.AccountMapper;
import com.bank.bankapi.metrics.BankMetrics;
import com.bank.bankapi.repository.AccountRepository;
import com.bank.bankapi.repository.UserRepository;
import com.bank.bankapi.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    private final AccountMapper accountMapper;

    private final UserRepository userRepository;

    private final BankMetrics bankMetrics;

    @CachePut(value = "accounts", key = "#result.id")
    @Transactional
    public AccountResponse create(CreateAccountRequest request) {

        log.info("Creating account for user {}", request.getUserId());
        User owner = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User ", request.getUserId()));

        Account account = new Account(owner, request.getBalance());

        Account saved = accountRepository.save(account);
        bankMetrics.incrementAccountsCreated();
        log.info("Account {} created successfully for {}", saved.getId(), owner.getUsername());
        return accountMapper.toResponse(saved);
    }

    @Override
    @Cacheable(value = "accounts", key = "#accountId")
    @Transactional(readOnly = true)
    public AccountResponse getById(UUID accountId) {
        log.info("Loading account {} from PostgreSQL", accountId);
        Account entity = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account ", accountId));
        return accountMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        log.info("Retrieving all accounts request");
        return accountRepository.findAll()
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @CacheEvict(value = "accounts", key = "#accountId")
    @Transactional
    @Override
    public AccountResponse deposit(UUID accountId, DepositRequest deposit) {
        log.info("Initiating deposit of {} into account {}", deposit.getDepositAmount(), accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account ", accountId));

        account.deposit(deposit.getDepositAmount());
        bankMetrics.incrementDepositsMade();
        log.info("Deposit of {} for {} completed", deposit.getDepositAmount(), accountId);
        return accountMapper.toResponse(account);
    }

    @CacheEvict(value = "accounts", key = "#accountId")
    @Transactional
    @Override
    public AccountResponse withdraw(UUID accountId, WithdrawRequest withdraw) {
        log.info("Initiating withdrawal of {} into account {}",
                withdraw.getWithdrawAmount(), accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account ", accountId));

        account.withdraw(withdraw.getWithdrawAmount());
        bankMetrics.incrementWithdrawalsMade();
        log.info("Withdrawal of {} for {} completed", withdraw.getWithdrawAmount(), accountId);
        return accountMapper.toResponse(account);
    }

    @CacheEvict(value = "accounts", key = "#accountId")
    @Transactional
    @Override
    public void delete(UUID accountId) {
        log.info("Deleting account {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account ", accountId));
        account.setDeleted(true);
        account.setDeletedAt(Instant.now());
        accountRepository.save(account);
        log.info("Account {} marked as deleted", accountId);

    }


}