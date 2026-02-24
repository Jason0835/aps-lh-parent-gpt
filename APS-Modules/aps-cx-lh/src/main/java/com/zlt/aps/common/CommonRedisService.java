package com.zlt.aps.common;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.maindata.utils.CxLhEngineUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 提供Redis缓存相关方法
 *
 * @author Nick
 */
@Component("CommonRedisService")
@Slf4j
public class CommonRedisService {

    @Autowired
    private RedisTemplate redisTemplate;

    //机台小时集合
    private Map<String,Double> machineShiftHourMap;



    private Map<String,String > cxParamsMap;

    /**
     *  班次开始时间
     */
    private Map<String,Date> classShiftDateTime;

    private String division = "\r\n---------------------------------------------------\r\n";  //日志分割符

    /**
     * 根据Key获取流水号
     * @param key
     * @return
     */
    public Long getIncrementNumber(String key){
        RedisAtomicLong entityIdCounter=new RedisAtomicLong(key,redisTemplate.getConnectionFactory());
        Long counter=entityIdCounter.incrementAndGet();
        if ((null == counter || counter.longValue() == 1)) {// 初始设置过期时间
            log.debug("【自动生成流水号】设置过期时间为7天!");
            entityIdCounter.expire(7, TimeUnit.DAYS);// 单位天
        }
        return counter;
    }

    /**
     * 判断key是否存在
     * @param key
     * @return
     */
    public boolean hasKey(String key){
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置key和过期时间设置
     * @param key key值
     * @param value 内容
     * @param time 超时时长
     * @param timeUnit 超时时间单位
     * @return
     */
    public Boolean setIfAbsent(String key,String value,Long time,TimeUnit timeUnit){
        return redisTemplate.opsForValue().setIfAbsent(key,value,time,timeUnit);
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
     * @param key
     * @param prefix
     * @return
     */
    public String getSequence(String key,String prefix){
        String factoryCode = StringUtils.isNotEmpty(SecurityUtils.getUserCurrentFactory()) ? SecurityUtils.getUserCurrentFactory() : FactoryConstant.DEFAULT_FACTORY_CODE;
        Long sequenceNo = getIncrementNumber(factoryCode+key + prefix);
        return CxLhEngineUtils.getSequence(prefix,sequenceNo);
    }

}
