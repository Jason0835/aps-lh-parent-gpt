package com.zlt.aps.cx.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 成型排程执行互斥保护组件。
 *
 * <p>按工厂+排程日期维度加分布式锁，避免同一排程窗口被并发执行。
 * <p>锁默认TTL 120分钟，排程正常结束后主动释放。
 *
 * @author APS Team
 */
@Slf4j
@Component
public class ScheduleExecutionGuard {

    private static final long DEFAULT_LOCK_TTL_MINUTES = 120L;
    private static final String LOCK_KEY_PREFIX = "APS:CX:SCHEDULE:LOCK:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取排程执行锁。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @return 锁令牌，获取失败返回 null（表示已有排程在执行中）
     */
    public String acquire(String factoryCode, LocalDate scheduleDate) {
        String lockKey = buildLockKey(factoryCode, scheduleDate);
        String token = UUID.randomUUID().toString();
        try {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, DEFAULT_LOCK_TTL_MINUTES, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(locked)) {
                log.info("成型排程执行锁获取成功, key: {}", lockKey);
                return token;
            }
            log.warn("成型排程执行锁已被占用, key: {}", lockKey);
            return null;
        } catch (Exception e) {
            log.error("成型排程执行锁操作异常, key: {}, 将允许排程继续执行", lockKey, e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 释放排程执行锁。
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排程日期
     * @param token        锁令牌
     */
    public void release(String factoryCode, LocalDate scheduleDate, String token) {
        if (token == null) {
            return;
        }
        String lockKey = buildLockKey(factoryCode, scheduleDate);
        try {
            Long result = stringRedisTemplate.execute(RELEASE_SCRIPT,
                    Collections.singletonList(lockKey), token);
            if (result != null && result > 0) {
                log.info("成型排程执行锁释放成功, key: {}", lockKey);
            } else {
                log.warn("成型排程执行锁释放失败（token不匹配或锁已过期）, key: {}", lockKey);
            }
        } catch (Exception e) {
            log.warn("成型排程执行锁释放异常, key: {}, 原因: {}", lockKey, e.getMessage());
        }
    }

    private String buildLockKey(String factoryCode, LocalDate scheduleDate) {
        String fc = factoryCode != null ? factoryCode : "DEFAULT";
        return LOCK_KEY_PREFIX + fc + ":" + scheduleDate.format(DATE_FORMATTER);
    }
}
