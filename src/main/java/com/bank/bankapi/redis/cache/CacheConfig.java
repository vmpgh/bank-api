package com.bank.bankapi.redis.cache;

import com.bank.bankapi.redis.imdepotency.IdempotencyRecord;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory
            ) {

        RedisSerializer<Object> serializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableUnsafeDefaultTyping()
                        .build();

        RedisCacheConfiguration configuration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .build();
    }

    @Bean
    public RedisTemplate<String, IdempotencyRecord> idempotencyRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {

        RedisTemplate<String, IdempotencyRecord> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        JacksonJsonRedisSerializer<IdempotencyRecord> valueSerializer =
                new JacksonJsonRedisSerializer<>(IdempotencyRecord.class);

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);

        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }
}


