package com.weave.global.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // 기본 캐시 설정 (TTL 10분)
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        // 피싱 패턴 캐시 (TTL 30분 - 자주 변경되지 않음)
        .withCacheConfiguration("phishingPatterns",
            defaultConfig.entryTtl(Duration.ofMinutes(30)))
        // 고위험 패턴 캐시 (TTL 30분)
        .withCacheConfiguration("highRiskPatterns",
            defaultConfig.entryTtl(Duration.ofMinutes(30)))
        // 위치 데이터 캐시 (TTL 30초 - 실시간성 중요)
        .withCacheConfiguration("locations",
            defaultConfig.entryTtl(Duration.ofSeconds(30)))
        // 공휴일 캐시 (TTL 24시간 - 거의 변경되지 않음)
        .withCacheConfiguration("holidays",
            defaultConfig.entryTtl(Duration.ofHours(24)))
        // 사용자 정보 캐시 (TTL 5분)
        .withCacheConfiguration("users",
            defaultConfig.entryTtl(Duration.ofMinutes(5)))
        // 워크스페이스 캐시 (TTL 5분)
        .withCacheConfiguration("workspaces",
            defaultConfig.entryTtl(Duration.ofMinutes(5)))
        .build();
  }
}
