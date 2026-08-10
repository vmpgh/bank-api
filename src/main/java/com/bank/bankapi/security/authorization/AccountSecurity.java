package com.bank.bankapi.security.authorization;

import com.bank.bankapi.entity.Account;
import com.bank.bankapi.exception.ResourceNotFoundException;
import com.bank.bankapi.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountSecurity {

    private final AccountRepository accountRepository;


    public boolean isOwner(UUID accountId,
                           Authentication authentication) {

        String username = authentication.getName();

        Account account =
                accountRepository.findById(accountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account",
                                        accountId.toString()));


        return account.getOwner()
                .getUsername()
                .equals(username);
    }
}