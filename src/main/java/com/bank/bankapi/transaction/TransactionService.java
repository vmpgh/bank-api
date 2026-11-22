package com.bank.bankapi.transaction;

import com.bank.bankapi.dto.TransactionResponse;
import com.bank.bankapi.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface TransactionService {

    /** Records a deposit. Call after {@link Account#deposit}, inside the same transaction. */
    void recordDeposit(Account account, BigDecimal amount);

    /** Records a withdrawal. Call after {@link Account#withdraw}, inside the same transaction. */
    void recordWithdrawal(Account account, BigDecimal amount);

    /** Records both legs of a transfer. Call after debit and credit, inside the same transaction. */
    void recordTransfer(Account sender, Account receiver, BigDecimal amount, UUID transferId, Instant at);

    /** Newest-first page of an account's transactions. */
    Page<TransactionResponse> getHistory(UUID accountId, TransactionHistoryFilter filter, Pageable pageable);
}
