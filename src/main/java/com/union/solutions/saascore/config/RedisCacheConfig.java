package com.union.solutions.saascore.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Cache Redis para respostas consumidas pelo front (ver docs CACHE-REDIS-FRONT no repo
 * fluxe-b2b-suite). frontTenants: GET /v1/tenants (lista), TTL 120s. Para desativar cache de
 * tenants (ex.: erro de deserialização Redis), use app.cache.front-tenants-enabled=false.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

  private static final Duration FRONT_TENANTS_TTL = Duration.ofSeconds(120);

  @Value("${app.cache.front-tenants-enabled:true}")
  private boolean frontTenantsCacheEnabled;

  @Bean
  public CacheManager cacheManager(
      @Autowired(required = false) RedisConnectionFactory connectionFactory) {
    if (connectionFactory == null || !frontTenantsCacheEnabled) {
      return new org.springframework.cache.concurrent.ConcurrentMapCacheManager();
    }
    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(FRONT_TENANTS_TTL)
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .withCacheConfiguration("frontTenants", config.entryTtl(FRONT_TENANTS_TTL))
        .build();
  }
}
