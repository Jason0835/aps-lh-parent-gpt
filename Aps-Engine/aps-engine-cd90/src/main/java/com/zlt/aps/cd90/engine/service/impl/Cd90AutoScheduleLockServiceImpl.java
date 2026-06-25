package com.zlt.aps.cd90.engine.service.impl;

import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleLockService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 直裁自动排程分布式锁服务实现。
 */
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleLockServiceImpl implements Cd90AutoScheduleLockService {

    private static final String LOCK_KEY_PREFIX = "aps:cd90:auto-schedule:";

    private final RedissonClient redissonClient;

    @Override
    public RLock getLock(String factoryCode, LocalDate scheduleDate) {
        String lockKey = LOCK_KEY_PREFIX + factoryCode + ":" + scheduleDate;
        return redissonClient.getLock(lockKey);
    }
}
