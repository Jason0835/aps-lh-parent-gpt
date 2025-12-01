package com.zlt.mix.common.engine.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 提供自增长流水号
 */
@Slf4j
public class IncrementService {

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 根据Key获取流水号
     * @param key
     * @return
     */
    public Long getIncrementNumber(String key){
        RedisAtomicLong entityIdCounter=new RedisAtomicLong(key,redisTemplate.getConnectionFactory());
        Long counter=entityIdCounter.incrementAndGet();
        if ((null == counter || counter.longValue() == 1)) {// 初始设置过期时间
            log.debug("设置过期时间为7天!");
            entityIdCounter.expire(7, TimeUnit.DAYS);// 单位天
        }
        return counter;
    }

    /**
     * 删除key值
     * @param key
     */
    public void delRedisKey(String key){
        redisTemplate.delete(key);
    }

    /**
     * 生成自增长3位长队的序列
     * @param prefix 前缀字符串
     * @return
     */
    public String getSequence3(String prefix) {
        return getSequence(prefix, 3);
    }

    /**
     * 生成自增长4位长队的序列
     * @param prefix
     * @return
     */
    public String getSequence4(String prefix) {
        return getSequence(prefix, 4);
    }

    /**
     * 根据key和前缀获取流水号
     * @param prefix 序列前缀
     * @param numLength  自增长数字的最大位数
     * @return
     */
    public String getSequence(String prefix, Integer numLength){
        if(numLength == null) {
            return null;
        }
        Long sequenceNo = getIncrementNumber(prefix);
        String format = "%0" + numLength +"d";
        return prefix + String.format(format, sequenceNo);
    }
}
