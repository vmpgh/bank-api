package com.bank.bankapi.controller;

import com.bank.bankapi.dto.TransferRequest;
import com.bank.bankapi.dto.TransferResponse;
import com.bank.bankapi.exception.TooManyRequestsException;
import com.bank.bankapi.redis.imdepotency.IdempotencyRecord;
import com.bank.bankapi.redis.imdepotency.IdempotencyService;
import com.bank.bankapi.redis.ratelimit.RateLimiterService;
import com.bank.bankapi.service.TransferService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/transfers")
public class TransferController {


    private final TransferService transferService;
    private final IdempotencyService idempotencyService;
    private final RateLimiterService rateLimiterService;


    @PostMapping
    @PreAuthorize(
            "hasRole('ADMIN') or @accountSecurity.isOwner(#request.senderAccountId, authentication)"
    )
    public ResponseEntity<TransferResponse> transfer(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody TransferRequest request
    ) {

        if (!rateLimiterService.allowRequest(authentication.getName())) {
            throw new TooManyRequestsException("Rate limit exceeded.");
        }

        if (!idempotencyService.reserve(key)) {

            log.info("Duplicate transfer request detected for idempotency key={}", key);

            IdempotencyRecord record =
                    idempotencyService.get(key)
                            .orElseThrow();

            return ResponseEntity.status(record.httpStatus()).build();
        }

        try {

            log.info("Reserved idempotency key={}, processing transfer", key);

            TransferResponse response =
                    transferService.transfer(request);

            idempotencyService.markSuccess(key);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            idempotencyService.remove(key);

            log.warn("Transfer failed, removing idempotency key={}", key);

            throw e;
        }
    }
}
