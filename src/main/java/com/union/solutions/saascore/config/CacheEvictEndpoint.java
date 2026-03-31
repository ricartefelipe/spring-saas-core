package com.union.solutions.saascore.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Actuator para limpar caches em memória (Spring Cache). A lista GET /v1/tenants já não usa região
 * dedicada em Redis.
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
    int cleared = 0;
    for (String name : cacheManager.getCacheNames()) {
      Cache cache = cacheManager.getCache(name);
      if (cache != null) {
        cache.clear();
        cleared++;
      }
    }
    return cleared > 0 ? ("cleared " + cleared + " cache region(s)") : "no cache regions";
  }
}
