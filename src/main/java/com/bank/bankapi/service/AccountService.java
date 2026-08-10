package com.bank.bankapi.service;

import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.DepositRequest;
import com.bank.bankapi.dto.WithdrawRequest;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    AccountResponse create(CreateAccountRequest request) ;

    AccountResponse getById(UUID id);

    List<AccountResponse> findAll()  ;

    void delete(UUID id);

    AccountResponse deposit(UUID id, DepositRequest deposit);

    AccountResponse withdraw(UUID id, WithdrawRequest deposit);

}
