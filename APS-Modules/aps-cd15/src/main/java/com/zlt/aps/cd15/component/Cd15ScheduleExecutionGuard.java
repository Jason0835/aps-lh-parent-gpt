package com.zlt.aps.cd15.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 按工厂和排程日期保护斜裁发布过程。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15ScheduleExecutionGuard {

    private static final String LOCK_KEY_PREFIX =
            "APS:CD15:ISSUE_TO_MES:LOCK:";
    private static final long LOCK_TTL_SECONDS = 60L;
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('del', KEYS[1]) "
                            + "else return 0 end",
                    Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 获取发布锁。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 锁令牌；锁已被占用或 Redis 异常时返回 null
     */
    public String acquireIssueLock(String factoryCode, Date scheduleDate) {
        String lockKey = this.buildLockKey(factoryCode, scheduleDate);
        String token = UUID.randomUUID().toString();
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey, token, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                log.info("斜裁排程发布锁获取成功, key={}", lockKey);
                return token;
            }
            log.warn("斜裁排程发布锁已被占用, key={}", lockKey);
            return null;
        } catch (Exception exception) {
            log.error("斜裁排程发布锁获取失败, key={}", lockKey, exception);
            return null;
        }
    }

    /**
     * 仅在令牌一致时释放发布锁。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param token 锁令牌
     */
    public void releaseIssueLock(String factoryCode, Date scheduleDate,
                                 String token) {
        if (token == null) {
            return;
        }
        String lockKey = this.buildLockKey(factoryCode, scheduleDate);
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT,
                    Collections.singletonList(lockKey), token);
            log.info("斜裁排程发布锁释放完成, key={}", lockKey);
        } catch (Exception exception) {
            log.warn("斜裁排程发布锁释放失败, key={}, reason={}",
                    lockKey, exception.getMessage());
        }
    }

    /** 构造发布锁键。 */
    private String buildLockKey(String factoryCode, Date scheduleDate) {
        return LOCK_KEY_PREFIX + factoryCode + ":"
                + DATE_FORMAT.get().format(scheduleDate);
    }
}
