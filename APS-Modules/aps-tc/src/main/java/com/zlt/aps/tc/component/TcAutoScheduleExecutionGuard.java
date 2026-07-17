package com.zlt.aps.tc.component;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 胎侧自动排程执行互斥保护组件。
 *
 * <p>生产环境优先使用 Redis 原子锁；测试环境未注入 Redis 时使用进程内互斥集合，
 * 确保同一工厂和排程日期不会并发执行核心排程。</p>
 */
@Slf4j
@Component
public class TcAutoScheduleExecutionGuard {

    private static final long LOCK_TTL_MINUTES = 120L;

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private static final Set<String> LOCAL_LOCK_KEY_SET = ConcurrentHashMap.newKeySet();

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建执行锁组件。
     *
     * @param stringRedisTemplate Redis 字符串模板，单元测试环境允许为空
     */
    @Autowired
    public TcAutoScheduleExecutionGuard(@Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取自动排程执行锁。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 锁令牌
     * @throws ServiceException 同维度任务正在执行或 Redis 加锁异常时抛出
     */
    public String acquire(String factoryCode, Date scheduleDate) {
        String lockKey = this.buildLockKey(factoryCode, scheduleDate);
        String token = UUID.randomUUID().toString();
        if (stringRedisTemplate == null) {
            if (LOCAL_LOCK_KEY_SET.add(lockKey)) {
                return token;
            }
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.concurrentTask"));
        }
        try {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, LOCK_TTL_MINUTES, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(locked)) {
                return token;
            }
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.concurrentTask"));
        } catch (ServiceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("[TC_AUTO_PLAN] 获取执行锁失败, lockKey={}", lockKey, exception);
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.lockFailed"));
        }
    }

    /**
     * 释放自动排程执行锁。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param token 锁令牌
     */
    public void release(String factoryCode, Date scheduleDate, String token) {
        if (token == null) {
            return;
        }
        String lockKey = this.buildLockKey(factoryCode, scheduleDate);
        if (stringRedisTemplate == null) {
            LOCAL_LOCK_KEY_SET.remove(lockKey);
            return;
        }
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (RuntimeException exception) {
            log.warn("[TC_AUTO_PLAN] 释放执行锁失败, lockKey={}, reason={}", lockKey, exception.getMessage());
        }
    }

    /**
     * 构建工厂和排程日期维度的 Redis 锁键。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return Redis 锁键
     */
    private String buildLockKey(String factoryCode, Date scheduleDate) {
        return TcScheduleConstants.AUTO_SCHEDULE_LOCK_KEY_PREFIX + factoryCode + ":"
                + DateUtil.formatDate(scheduleDate);
    }
}
