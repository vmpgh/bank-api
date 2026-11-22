-- Append-only ledger: one row per balance change on an account.
-- A transfer writes two rows (TRANSFER_OUT on the sender, TRANSFER_IN on the receiver)
-- that share the same transfer_id (the TransferCompletedEvent id).
CREATE TABLE account_transactions
(
    id UUID PRIMARY KEY,

    account_id UUID NOT NULL,

    type VARCHAR(20) NOT NULL,

    amount NUMERIC(19,2) NOT NULL,

    balance_after NUMERIC(19,2) NOT NULL,

    counterparty_account_id UUID,

    transfer_id UUID,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_account_transactions_account
        FOREIGN KEY (account_id)
            REFERENCES accounts(id),

    CONSTRAINT chk_account_transactions_type
        CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')),

    CONSTRAINT chk_account_transactions_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_account_transactions_account_created
    ON account_transactions(account_id, created_at DESC);
