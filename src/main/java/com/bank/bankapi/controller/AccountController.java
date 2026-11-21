package com.bank.bankapi.controller;

import com.bank.bankapi.dto.CreateAccountRequest;
import com.bank.bankapi.dto.AccountResponse;
import com.bank.bankapi.dto.DepositRequest;
import com.bank.bankapi.dto.PageResponse;
import com.bank.bankapi.dto.TransactionResponse;
import com.bank.bankapi.dto.WithdrawRequest;
import com.bank.bankapi.service.AccountService;
import com.bank.bankapi.transaction.TransactionHistoryFilter;
import com.bank.bankapi.transaction.TransactionService;
import com.bank.bankapi.transaction.TransactionType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService service;

    private final TransactionService transactionService;

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

    /**
     * Newest-first transaction history of one account.
     * {@code from} is inclusive and {@code to} exclusive (ISO-8601 instants).
     */
    @PreAuthorize(
            "hasRole('ADMIN') or @accountSecurity.isOwner(#id, authentication)"
    )
    @GetMapping("/{id}/transactions")
    public PageResponse<TransactionResponse> getTransactions(
            @PathVariable UUID id,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        TransactionHistoryFilter filter = new TransactionHistoryFilter(type, from, to);

        return PageResponse.from(
                transactionService.getHistory(id, filter, PageRequest.of(page, size)));
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