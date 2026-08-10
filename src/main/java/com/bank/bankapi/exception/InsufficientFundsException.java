package com.bank.bankapi.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID accountId){
        super("Account " + accountId + " has insufficient funds.");
    }

}
