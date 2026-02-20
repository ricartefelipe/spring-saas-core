package com.yourorg.saascore.application.ratelimit;

public interface RateLimitService {

    RateLimitResult tryConsume(String key, int limitPerMinute);

    record RateLimitResult(boolean allowed, int limit, int remaining, int retryAfterSeconds) {}
}
