/*
package com.bank.bankapi.metrics;


public class MetricsRedisCache {

    @Override
    public ValueWrapper get(Object key) {

        ValueWrapper value = delegate.get(key);

        if (value == null) {
            cacheMetrics.incrementMisses();
        } else {
            cacheMetrics.incrementHits();
        }

        return value;
    }

    @Override
    public void put(Object key, Object value) {

        delegate.put(key, value);

        cacheMetrics.incrementPuts();
    }

    @Override
    public void evict(Object key) {

        delegate.evict(key);

        cacheMetrics.incrementEvictions();
    }


}
*/
