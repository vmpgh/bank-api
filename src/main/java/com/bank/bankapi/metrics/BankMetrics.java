package com.bank.bankapi.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BankMetrics {


    private final Counter accountsCreated;
    private final Counter deposits;
    private final Counter withdrawals;
    private final Counter transfers;
    private final Counter failedTransfers;

    public BankMetrics(MeterRegistry meterRegistry) {

        this.accountsCreated =
                Counter.builder("bank.accounts.created.total")
                        .description("Total bank accounts created")
                        .register(meterRegistry);

        this.deposits =
                Counter.builder("bank.deposits.total")
                        .description("Total bank deposits made")
                        .register(meterRegistry);

        this.withdrawals =
                Counter.builder("bank.withdrawals.total")
                        .description("Total bank withdrawals made")
                        .register(meterRegistry);

        this.transfers =
                Counter.builder("bank.transfers.success.total")
                        .description("Total bank transfers made")
                        .register(meterRegistry);

        this.failedTransfers =
                Counter.builder("bank.transfers.fail.total")
                        .description("Total failed bank transfers")
                        .register(meterRegistry);
    }

    public void incrementAccountsCreated() {
        accountsCreated.increment();
    }

    public void incrementDepositsMade() {
        deposits.increment();
    }
    public void incrementWithdrawalsMade() {
        withdrawals.increment();
    }
    public void incrementTransfersMade() {
        transfers.increment();
    }
    public void incrementFailedTransferAttempts() {
        failedTransfers.increment();
    }

}

