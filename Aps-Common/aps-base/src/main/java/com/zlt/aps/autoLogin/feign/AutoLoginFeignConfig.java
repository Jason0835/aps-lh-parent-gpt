package com.zlt.aps.autoLogin.feign;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhangxh
 * @date 20250506
 * @description 同步服务Feign配置
 * 同步服务Feign配置
 */
@Configuration
public class AutoLoginFeignConfig {
    @Bean
    public DynamicHeaderInterceptor dynamicHeaderInterceptor() {
        return new DynamicHeaderInterceptor();
    }
}