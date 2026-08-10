package com.bank.bankapi.redis.imdepotency;

import java.util.Optional;

public interface IdempotencyService {

    boolean reserve(String key);

    void markSuccess(String key);

    void remove(String key);

    Optional<IdempotencyRecord> get(String key);

}
