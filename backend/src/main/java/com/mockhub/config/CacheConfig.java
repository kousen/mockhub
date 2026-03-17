package com.mockhub.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Uses default ConcurrentMapCacheManager from spring-boot-starter-cache.
    // Redis can be swapped in later by adding spring-boot-starter-data-redis.
}
