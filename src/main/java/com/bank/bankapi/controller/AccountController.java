package com.bank.bankapi.controller;

import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.DepositRequest;
import com.bank.bankapi.dto.WithdrawRequest;
import com.bank.bankapi.service.AccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return service.create(request);
    }

    @PreAuthorize(
            "hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)"
    )
    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id){
        return service.getById(id);
    }

    @PreAuthorize(
            "hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)"
    )
    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable UUID id,
                                   @Valid @RequestBody DepositRequest deposit){

        return service.deposit(id,deposit);
    }

    @PreAuthorize(
            "hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)"
    )
    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable UUID id,
                                   @Valid @RequestBody WithdrawRequest withdraw){

        return service.withdraw(id,withdraw);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AccountResponse> getAll() {
        return service.findAll();
    }


    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable UUID   id){
        service.delete(id);
    }
}