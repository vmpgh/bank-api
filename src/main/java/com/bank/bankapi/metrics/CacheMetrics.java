package com.bank.bankapi.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CacheMetrics {

    private final Counter hits;
    private final Counter misses;
    private final Counter puts;
    private final Counter evictions;

    public CacheMetrics(MeterRegistry registry) {

        hits = Counter.builder("bank.cache.hit.total")
                .description("Total cache hits")
                .register(registry);

        misses = Counter.builder("bank.cache.miss.total")
                .description("Total cache misses")
                .register(registry);

        puts = Counter.builder("bank.cache.put.total")
                .description("Total cache writes")
                .register(registry);

        evictions = Counter.builder("bank.cache.eviction.total")
                .description("Total cache evictions")
                .register(registry);
    }

    public void hit() {
        hits.increment();
    }

    public void miss() {
        misses.increment();
    }

    public void put() {
        puts.increment();
    }

    public void evict() {
        evictions.increment();
    }
}
