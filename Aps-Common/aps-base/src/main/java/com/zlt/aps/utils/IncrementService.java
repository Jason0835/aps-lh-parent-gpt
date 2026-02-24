package com.zlt.aps.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 提供自增长流水号
 */
@Slf4j
@Component
public class IncrementService {

    @Resource
    private RedisTemplate redisTemplate;

    @Value("${rowNo.expire.minutes}")
    private Integer defaultMins;

    /**
     * 根据Key获取流水号
     * @param key
     * @return
     */
    public Long getIncrementNumber(String key){
        RedisAtomicLong entityIdCounter=new RedisAtomicLong(key,redisTemplate.getConnectionFactory());
        Long counter=entityIdCounter.incrementAndGet();
        if ((null == counter || counter.longValue() == 1)) {// 初始设置过期时间
            log.debug("设置过期时间为10分钟!");
            entityIdCounter.expire(defaultMins, TimeUnit.MINUTES);// 10分钟失效
        }
        return counter;
    }

    /**
     * 根据key和过期时间获取流水号
     *
     * @param key        key
     * @param expireTime 过期时间（分钟）
     * @return 流水号
     */
    public Long getIncrementNumberByExpire(String key, Integer expireTime) {
        RedisAtomicLong entityIdCounter = new RedisAtomicLong(key, redisTemplate.getConnectionFactory());
        Long counter = entityIdCounter.incrementAndGet();
        if ((null == counter || counter.longValue() == 1)) {// 初始设置过期时间
            log.debug("设置过期时间为{}分钟!", expireTime);
            entityIdCounter.expire(expireTime, TimeUnit.MINUTES);// 10分钟失效
        }
        return counter;
    }

    /**
     * 根据key、前缀、过期时间获取流水号
     *
     * @param prefix     序列前缀
     * @param numLength  自增长数字的最大位数
     * @param expireTime 过期时间（分钟）
     * @return 流水号
     */
    public String getBillNoSequenceByExpire(String prefix, Integer numLength, Integer expireTime) {
        if (numLength == null) {
            return null;
        }
        Long sequenceNo = getIncrementNumberByExpire(prefix, expireTime);
        String format = "%0" + numLength + "d";
        return prefix + String.format(format, sequenceNo);
    }

    /**
     * 删除key值
     * @param key
     */
    public void delRedisKey(String key){
        redisTemplate.delete(key);
    }


    /**
     * 根据key和前缀获取流水号
     * @param prefix 序列前缀
     * @param numLength  自增长数字的最大位数
     * @return
     */
    public String getBillNoSequence(String prefix, Integer numLength){
        if(numLength == null) {
            return null;
        }
        Long sequenceNo = getIncrementNumber(prefix);
        String format = "%0" + numLength +"d";
        return prefix + String.format(format, sequenceNo);
    }

    /**
     * 根据单据号获取行号
     * @param billCode 单据号
     * @param length 行号长度
     * @return
     */
    public String getBillRowNo(String billCode,Integer length){
        if(length == null) {
            return null;
        }
        Long sequenceNo = getBillRowIndex(billCode);
        String format = "%0" + length +"d";
        return String.format(format, sequenceNo);
    }

    /**
     * 根据单据号生成行号顺序
     * @param billCode
     * @return
     */
    public Long getBillRowIndex(String billCode){
        return getIncrementNumber(billCode);
    }

}
