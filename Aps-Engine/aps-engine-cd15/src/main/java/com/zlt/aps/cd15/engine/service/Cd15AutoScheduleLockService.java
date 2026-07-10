package com.zlt.aps.cd15.engine.service;

import org.redisson.api.RLock;

import java.time.LocalDate;

/**
 * 斜裁自动排程分布式锁服务。
 */
public interface Cd15AutoScheduleLockService {

    /**
     * 获取工厂和排程日期维度的分布式锁对象。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return Redisson锁对象
     */
    RLock getLock(String factoryCode, LocalDate scheduleDate);
}
