package com.zlt.aps.cd15.engine.service.impl;

import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleLockService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 斜裁自动排程分布式锁服务实现。
 */
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleLockServiceImpl implements Cd15AutoScheduleLockService {

    private static final String LOCK_KEY_PREFIX = "aps:cd15:auto-schedule:";

    private final RedissonClient redissonClient;

    @Override
    public RLock getLock(String factoryCode, LocalDate scheduleDate) {
        String lockKey = LOCK_KEY_PREFIX + factoryCode + ":" + scheduleDate;
        return redissonClient.getLock(lockKey);
    }
}
