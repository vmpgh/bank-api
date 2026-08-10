package com.bank.bankapi.redis.lock;

public interface DistributedLockService {

    boolean acquireLock(String lockName);

    void releaseLock(String lockName);

}
