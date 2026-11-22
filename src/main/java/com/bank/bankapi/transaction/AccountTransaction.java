package com.bank.bankapi.transaction;

import com.bank.bankapi.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One immutable ledger entry for a single balance change on an account.
 * <p>
 * The factory methods read {@code balanceAfter} from the account, so they must be
 * called <b>after</b> the balance has been changed.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "account_transactions")
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(updatable = false)
    private UUID counterpartyAccountId;

    @Column(updatable = false)
    private UUID transferId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private AccountTransaction(Account account,
                               TransactionType type,
                               BigDecimal amount,
                               UUID counterpartyAccountId,
                               UUID transferId,
                               Instant createdAt) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = account.getBalance();
        this.counterpartyAccountId = counterpartyAccountId;
        this.transferId = transferId;
        this.createdAt = createdAt;
    }

    public static AccountTransaction deposit(Account account, BigDecimal amount, Instant at) {
        return new AccountTransaction(account, TransactionType.DEPOSIT, amount, null, null, at);
    }

    public static AccountTransaction withdrawal(Account account, BigDecimal amount, Instant at) {
        return new AccountTransaction(account, TransactionType.WITHDRAWAL, amount, null, null, at);
    }

    public static AccountTransaction transferOut(Account sender, UUID receiverId,
                                                 BigDecimal amount, UUID transferId, Instant at) {
        return new AccountTransaction(sender, TransactionType.TRANSFER_OUT, amount, receiverId, transferId, at);
    }

    public static AccountTransaction transferIn(Account receiver, UUID senderId,
                                                BigDecimal amount, UUID transferId, Instant at) {
        return new AccountTransaction(receiver, TransactionType.TRANSFER_IN, amount, senderId, transferId, at);
    }
}
