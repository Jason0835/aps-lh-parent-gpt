package com.zlt.aps.monthplan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存配置属性
 * 绑定配置文件中前缀为 cache.caffeine 的配置项
 */
@Component
@Data
@ConfigurationProperties(prefix = "cache.caffeine")
public class CaffeineCacheProperties {

    /**
     * 缓存开关
     * 默认值：false（关闭缓存）
     */
    private boolean cacheEnabled = false;

}