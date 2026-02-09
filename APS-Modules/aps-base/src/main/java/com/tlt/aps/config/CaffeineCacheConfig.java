package com.tlt.aps.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类：Spring Cache + Caffeine
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {

    /**
     * 配置 Caffeine 缓存实例
     * @return Caffeine 缓存配置
     */
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
                // 缓存最大容量（超出后会按LRU策略淘汰）
                .maximumSize(100000)
                // 写入后过期时间
                .expireAfterWrite(24, TimeUnit.HOURS)
                // 访问后过期时间（增强缓存时效性）
                .expireAfterAccess(30, TimeUnit.MINUTES)
                // 开启缓存统计（用于监控缓存命中率）
                .recordStats();
    }

    /**
     * 配置 Spring CacheManager，使用 Caffeine 作为底层实现
     * @param caffeine Caffeine 配置实例
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        // 不允许缓存null值
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }
}