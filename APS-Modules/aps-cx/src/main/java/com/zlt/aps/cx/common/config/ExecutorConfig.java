package com.zlt.aps.cx.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 线程池相关配置类
 */
@Configuration
public class ExecutorConfig {

    @Bean("executorService")
    public ExecutorService executorService(){
        ExecutorService executor = Executors.newCachedThreadPool();
        return executor;
    }
}
