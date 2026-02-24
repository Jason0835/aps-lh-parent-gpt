package com.zlt.aps.mp.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 分布式版本生成器
 * @author wengpc
 */
@Component
@RequiredArgsConstructor
public class DistributedVersionGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String REDIS_KEY_PREFIX = ":version:seq:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成分布式唯一版本号
     * 利用 Redis 的 INCR 命令实现分布式全局唯一流水号，每日自动重置
     * @return 格式：prefix + 年月日 + 3位流水号
     */
    public String generateVersion(String prefix) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String redisKey = prefix + REDIS_KEY_PREFIX + today;
        // Redis原子自增，设置过期时间为1天（保证当日有效，次日自动重置）
        Long seq = stringRedisTemplate.opsForValue().increment(redisKey, 1);
        // 首次生成时设置过期时间
        if (seq != null && seq == 1) {
            stringRedisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
        }
        // 处理流水号（补零）
        int seqInt = seq != null ? seq.intValue() : 1;
        return String.format("%s%s%03d", prefix, today, seqInt);
    }
}