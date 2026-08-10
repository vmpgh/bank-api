package com.bank.bankapi.redis.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDistributedLockService implements DistributedLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    /**
     * Keeps track of locks owned by THIS application instance.
     */
    private final ConcurrentHashMap<String, String> ownedLocks =
            new ConcurrentHashMap<>();

    @Override
    public boolean acquireLock(String lockName) {

        String key = "lock:" + lockName;
        String ownerId = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                key,
                ownerId,
                LOCK_TTL
        );

        if (Boolean.TRUE.equals(acquired)) {
            ownedLocks.put(lockName, ownerId);
            log.info("Acquired lock '{}' owner={}", lockName, ownerId);
            return true;
        }

        log.debug("Lock '{}' already acquired by another instance", lockName);
        return false;
    }

    @Override
    public void releaseLock(String lockName) {

        String key = "lock:" + lockName;

        String ownerId = ownedLocks.get(lockName);

        if (ownerId == null) {
            log.warn("Attempted to release lock '{}' but no owner was found", lockName);
            return;
        }

        String currentOwner = redisTemplate.opsForValue().get(key);

        if (ownerId.equals(currentOwner)) {

            redisTemplate.delete(key);
            ownedLocks.remove(lockName);

            log.info("Released lock '{}' owner={}", lockName, ownerId);

        } else {

            log.warn("""
                    Lock '{}' is no longer owned by this instance.
                    Expected owner={}
                    Current owner={}
                    """,
                    lockName,
                    ownerId,
                    currentOwner);

            ownedLocks.remove(lockName);
        }
    }
}
