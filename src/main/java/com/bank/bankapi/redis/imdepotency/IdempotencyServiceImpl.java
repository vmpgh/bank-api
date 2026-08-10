package com.bank.bankapi.redis.imdepotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Duration PROCESSING_TTL = Duration.ofSeconds(30);
    private static final Duration SUCCESS_TTL = Duration.ofHours(24);
    private final RedisTemplate<String, IdempotencyRecord> redisTemplate;
    @Override
    public boolean reserve(String key) {
        return Boolean.TRUE.equals(

                redisTemplate.opsForValue().setIfAbsent(

                        "idempotency:" + key,

                        new IdempotencyRecord("PROCESSING", 202),

                        PROCESSING_TTL

                )

        );

    }


    @Override
    public void markSuccess(String key) {
        redisTemplate.opsForValue().set(

                "idempotency:" + key,

                new IdempotencyRecord("SUCCESS", HttpStatus.OK.value()),

                SUCCESS_TTL

        );
    log.info("Transfer completed successfully, marking idempotency key={} as SUCCESS", key);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete("idempotency:" + key);
        log.warn("Transfer failed, removing idempotency key={}", key);
    }

    @Override
    public Optional<IdempotencyRecord> get(String key) {
        String shortKey = key.substring(0, 8);
        return Optional.ofNullable(

                (IdempotencyRecord)

                        redisTemplate.opsForValue().get("idempotency:" + key)
        );

    }
}
