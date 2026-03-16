package com.zlt.aps.mp.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis序列号存储器
 * @author Yelq
 */
@Service
public class RedisSequenceStorageService {

  private static final String SEQUENCE_KEY_PREFIX = "req:seq:";
  private static final String DAILY_COUNTER_KEY_PREFIX = "req:daily:counter:";
  // 24小时
  private static final int SEQUENCE_EXPIRE_SECONDS = 24 * 60 * 60;

  @Autowired
  private StringRedisTemplate redisTemplate;

  private ValueOperations<String, String> valueOps;

  // Lua脚本：原子递增并设置过期时间
  private static final String INCREMENT_SCRIPT =
      "local key = KEYS[1]\n" +
          "local expire = ARGV[1]\n" +
          "local seq = redis.call('INCR', key)\n" +
          "if seq == 1 then\n" +
          "    redis.call('EXPIRE', key, expire)\n" +
          "end\n" +
          "return seq";

  private DefaultRedisScript<Long> incrementScript;

  @PostConstruct
  public void init() {
    valueOps = redisTemplate.opsForValue();
    incrementScript = new DefaultRedisScript<>();
    incrementScript.setScriptText(INCREMENT_SCRIPT);
    incrementScript.setResultType(Long.class);
  }

  /**
   * 原子递增序列号
   */
  public long incrementAndGet(String dateStr) {
    String key = SEQUENCE_KEY_PREFIX + dateStr;
    return redisTemplate.execute(
        incrementScript,
        Collections.singletonList(key),
        String.valueOf(SEQUENCE_EXPIRE_SECONDS)
    );
  }

  /**
   * 获取当前序列号
   */
  public long getCurrentSequence(String dateStr) {
    String key = SEQUENCE_KEY_PREFIX + dateStr;
    String value = valueOps.get(key);
    return value != null ? Long.parseLong(value) : 0;
  }

  /**
   * 重置序列号
   */
  public void resetSequence(String dateStr) {
    String key = SEQUENCE_KEY_PREFIX + dateStr;
    redisTemplate.delete(key);
  }

  /**
   * 记录每日生成计数
   */
  public void incrementDailyCounter() {
    String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String counterKey = DAILY_COUNTER_KEY_PREFIX + dateStr;
    redisTemplate.opsForValue().increment(counterKey);
    // 保留1天
    redisTemplate.expire(counterKey, 1, TimeUnit.DAYS);
  }

  /**
   * 获取每日生成数量
   */
  public long getDailyCount(String dateStr) {
    String counterKey = DAILY_COUNTER_KEY_PREFIX + dateStr;
    String value = redisTemplate.opsForValue().get(counterKey);
    return value != null ? Long.parseLong(value) : 0;
  }
}
