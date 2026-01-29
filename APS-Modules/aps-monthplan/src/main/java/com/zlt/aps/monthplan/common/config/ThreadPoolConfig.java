package com.zlt.aps.monthplan.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池
 * @author Yelq
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("ioExecutor")
    public Executor ioExecutor() {
      // I/O密集型任务：线程数 = CPU核心数 * 2 + 1
      int coreSize = Runtime.getRuntime().availableProcessors() * 2 + 1;
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(coreSize);
      executor.setMaxPoolSize(coreSize * 2);
      executor.setQueueCapacity(1000);
      executor.setThreadNamePrefix("async-io-");
      executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
      executor.initialize();
      return executor;
    }
}
