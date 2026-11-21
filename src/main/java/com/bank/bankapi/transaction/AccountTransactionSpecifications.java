package com.bank.bankapi.transaction;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

/**
 * Query building blocks for transaction history.
 * Optional filters return a {@code null} predicate when not set, which JPA ignores.
 */
public final class AccountTransactionSpecifications {

    private AccountTransactionSpecifications() {
    }

    public static Specification<AccountTransaction> forAccount(UUID accountId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<AccountTransaction> hasType(TransactionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    /** Inclusive lower bound. */
    public static Specification<AccountTransaction> createdFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    /** Exclusive upper bound. */
    public static Specification<AccountTransaction> createdBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThan(root.get("createdAt"), to);
    }

    public static Specification<AccountTransaction> matching(UUID accountId, TransactionHistoryFilter filter) {
        return forAccount(accountId)
                .and(hasType(filter.type()))
                .and(createdFrom(filter.from()))
                .and(createdBefore(filter.to()));
    }
}
