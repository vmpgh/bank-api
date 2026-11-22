package com.bank.bankapi.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountTransactionRepository
        extends JpaRepository<AccountTransaction, UUID>,
                JpaSpecificationExecutor<AccountTransaction> {
}
