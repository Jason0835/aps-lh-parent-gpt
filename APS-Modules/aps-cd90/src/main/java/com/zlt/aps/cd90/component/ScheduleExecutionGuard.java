package com.zlt.aps.cd90.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 直裁排程发布互斥保护组件。
 *
 * <p>按工厂+排程日期维度加分布式锁，避免同一发布窗口被并发执行。
 * 锁键格式：APS:CD90:ISSUE_TO_MES:LOCK:{factoryCode}:{scheduleDate}。
 * 锁 TTL 60 秒，覆盖一次 MES 下发+重试的合理上限。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class ScheduleExecutionGuard {

    private static final String ISSUE_LOCK_KEY_PREFIX = "APS:CD90:ISSUE_TO_MES:LOCK:";
    private static final long ISSUE_LOCK_TTL_SECONDS = 60L;
    private static final ThreadLocal<SimpleDateFormat> DATE_FMT = ThreadLocal.withInitial(
            () -> new SimpleDateFormat("yyyy-MM-dd"));

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取发布锁。锁键按工厂+排程日期隔离，不同工厂或不同日期可并发。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 锁令牌；获取失败返回 null（已有并发发布在执行）
     */
    public String acquireIssueLock(String factoryCode, Date scheduleDate) {
        String lockKey = buildLockKey(factoryCode, scheduleDate);
        String token = UUID.randomUUID().toString();
        try {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, ISSUE_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                log.info("直裁排程发布锁获取成功, key: {}", lockKey);
                return token;
            }
            log.warn("直裁排程发布锁已被占用, key: {}", lockKey);
            return null;
        } catch (Exception e) {
            log.error("直裁排程发布锁获取失败, key: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放发布锁。仅当 token 匹配时才释放，避免误释放过期后被他方获取的锁。
     */
    public void releaseIssueLock(String factoryCode, Date scheduleDate, String token) {
        if (token == null) {
            return;
        }
        String lockKey = buildLockKey(factoryCode, scheduleDate);
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(lockKey), token);
            log.info("直裁排程发布锁释放完成, key: {}", lockKey);
        } catch (Exception e) {
            log.warn("直裁排程发布锁释放失败, key: {}, 原因: {}", lockKey, e.getMessage());
        }
    }

    private String buildLockKey(String factoryCode, Date scheduleDate) {
        return ISSUE_LOCK_KEY_PREFIX + factoryCode + ":" + formatDate(scheduleDate);
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "null";
        }
        return DATE_FMT.get().format(date);
    }
}
