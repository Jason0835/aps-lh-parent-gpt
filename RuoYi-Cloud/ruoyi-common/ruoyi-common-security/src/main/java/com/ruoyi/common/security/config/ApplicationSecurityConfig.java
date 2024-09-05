package com.ruoyi.common.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

/**
 * 系统配置
 *
 * @author ruoyi
 */
@Component
public class ApplicationSecurityConfig {

    /***
     * 当前系统的代号，这个用来验证权限，指定一个系统的权限
     */
    @Value("${system.currentCode}")
    private String currentCode;

    public String CurrentCode() {
        return currentCode;
    }

    /**
     * 时区配置
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    }
}
