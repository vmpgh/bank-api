package com.bank.bankapi.redis.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisRateLimiterService implements RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${bank.rate-limit.transfer.limit}")
    private int limit;

    @Value("${bank.rate-limit.transfer.window}")
    private long window;

    @Override
    public boolean allowRequest(String username) {

        String key = "rate_limit:" + username;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count > limit) {
            log.warn("Rate limit exceeded for user={} count={}", username, count);
            return false;
        }
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(window));
        }
        log.info("Rate limit user={} count={}/{}", username, count, limit);

        return count != null && count <= limit;

    }
}
