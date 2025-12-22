package com.zlt.sync.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zlt.sync.scheduled.AbstractScheduler;
import com.zlt.sync.scheduled.impl.RedisSchedulerImpl;
import com.zlt.sync.utils.RedisLock;

@Configuration
public class SyncScheduleConfig {
    @Bean
    public AbstractScheduler getScheduler() {
        System.out.println("RedisSchedulerImpl === ");
        return new RedisSchedulerImpl();
    }

    @Bean
    public RedisLock getRedisLock() {
        return new RedisLock();
    }
}
