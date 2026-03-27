package com.union.solutions.saascore.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache com armazenamento em memória.
 *
 * <p>A lista GET /v1/tenants não é cacheada em Redis: serializar {@code List<Tenant>} no Redis
 * gerava ClassCastException ou SerializationException (formato legado vs default typing). O custo
 * da lista na BD é aceitável frente à robustez.
 *
 * <p>O Redis continua disponível para outras funções (ex.: health).
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

  @Bean
  public CacheManager cacheManager() {
    return new ConcurrentMapCacheManager();
  }
}
