package com.bank.bankapi.redis.ratelimit;

public interface RateLimiterService {

    boolean allowRequest(String key);

}