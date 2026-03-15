package com.union.solutions.saascore.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Actuator endpoint para limpar o cache frontTenants (GET /v1/tenants). Útil após deploy quando o
 * formato de serialização do cache mudou.
 *
 * <p>POST /actuator/cacheevict
 */
@Component
@Endpoint(id = "cacheevict")
public class CacheEvictEndpoint {

  private final CacheManager cacheManager;

  public CacheEvictEndpoint(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @WriteOperation
  public String evictFrontTenants() {
    Cache cache = cacheManager.getCache("frontTenants");
    if (cache != null) {
      cache.clear();
      return "frontTenants cleared";
    }
    return "frontTenants not available (cache disabled or not Redis)";
  }
}
