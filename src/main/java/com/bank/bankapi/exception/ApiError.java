package com.bank.bankapi.exception;


import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ApiError(    LocalDateTime timestamp,
                           int status,
                           String error,
                           String message,
                           String path,
                           Map<String,String> errors
) {
}
