package com.zlt.sync.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RedisLock {

    private static final Logger logger = LoggerFactory.getLogger(RedisLock.class);

    @Autowired
    private RedisTemplate redisTemplate;

    private static Long LOCK_SUCCESS = 1L;
    private static Long RELEASE_SUCCESS = 1L;
    private static Long LOCK_EXPIRED = -1L;

    private static final String LOCK_PREFIX = "REDIS_LOCK";

    //定义获取锁的lua脚本
    private final static DefaultRedisScript<Long> LOCK_LUA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call(\"setnx\", KEYS[1], KEYS[2]) == 1 then return redis.call(\"pexpire\", KEYS[1], KEYS[3]) else return 0 end"
            , Long.class
    );

    //定义释放锁的lua脚本
    private final static DefaultRedisScript<Long> UNLOCK_LUA_SCRIPT = new DefaultRedisScript<>(
            "if redis.call(\"get\",KEYS[1]) == KEYS[2] then return redis.call(\"del\",KEYS[1]) else return -1 end"
            , Long.class
    );

    private final static ThreadLocal<String> localRequestIds = new ThreadLocal<String>();
    private final static ThreadLocal<String> localKeys = new ThreadLocal<String>();

    /**
     * 加锁
     * @param key Key
     * @param timeout 过期时间
     * @param retryTimes 重试次数
     * @return
     */
    public boolean lock(String key, long timeout, int retryTimes) {
        try {
            final String redisKey = this.getRedisKey(key);
            final String requestId = this.getRequestId();
            logger.info("lock :::: redisKey = " + redisKey + " requestid = " + requestId);
            //组装lua脚本参数
            List<String> keys = Arrays.asList(redisKey, requestId, String.valueOf(timeout));
            //执行脚本
            Long result = (Long)redisTemplate.execute(LOCK_LUA_SCRIPT, keys);
            //存储本地变量
            if(!StringUtils.isEmpty(result) && result == LOCK_SUCCESS) {
                localRequestIds.set(requestId);
                localKeys.set(redisKey);
                logger.info("success to acquire lock:" + Thread.currentThread().getName() + ", Status code reply:" + result);
                return true;
            } else if (retryTimes == 0) {
                //重试次数为0直接返回失败
                return false;
            } else {
                //重试获取锁
                logger.info("retry to acquire lock:" + Thread.currentThread().getName() + ", Status code reply:" + result);
                int count = 0;
                while(true) {
                    try {
                        //休眠一定时间后再获取锁，这里时间可以通过外部设置
                        Thread.sleep(100);
                        result = (Long)redisTemplate.execute(LOCK_LUA_SCRIPT, keys);
                        if(!StringUtils.isEmpty(result) && result == LOCK_SUCCESS) {
                            localRequestIds.set(requestId);
                            localKeys.set(redisKey);
                            logger.info("success to acquire lock:" + Thread.currentThread().getName() + ", Status code reply:" + result);
                            return true;
                        } else {
                            count++;
                            if (retryTimes == count) {
                                logger.info("fail to acquire lock for " + Thread.currentThread().getName() + ", Status code reply:" + result);
                                return false;
                            } else {
                                logger.warn(count + " times try to acquire lock for " + Thread.currentThread().getName() + ", Status code reply:" + result);
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("acquire redis occured an exception:" + Thread.currentThread().getName(), e);
                        break;
                    }
                }
            }
        } catch (Exception e1) {
            logger.error("acquire redis occured an exception:" + Thread.currentThread().getName(), e1);
        }
        return false;
    }

    /**
     * 获取RedisKey
     * @param key 原始KEY，如果为空，自动生成随机KEY
     * @return
     */
    private String getRedisKey(String key) {
        //如果Key为空且线程已经保存，直接用，异常保护
        if (StringUtils.isEmpty(key) && !StringUtils.isEmpty(localKeys.get())) {
            return localKeys.get();
        }
        //如果都是空那就抛出异常
        if (StringUtils.isEmpty(key) && StringUtils.isEmpty(localKeys.get())) {
            throw new RuntimeException("key is null");
        }
        return LOCK_PREFIX + key;
    }

    /**
     * 获取随机请求ID
     * @return
     */
    private String getRequestId() {
        return UUID.randomUUID().toString();
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * 释放KEY
     * @param key
     * @return
     */
    public boolean unlock(String key) {
        try {
            String localKey = localKeys.get();
            //如果本地线程没有KEY，说明还没加锁，不能释放
            if(StringUtils.isEmpty(localKey)) {
                logger.error("release lock occured an error: lock key not found");
                return false;
            }
            String redisKey = getRedisKey(key);
            //判断KEY是否正确，不能释放其他线程的KEY
            if(!StringUtils.isEmpty(localKey) && !localKey.equals(redisKey)) {
                logger.error("release lock occured an error: illegal key:" + key);
                return false;
            }
            //组装lua脚本参数
            List<String> keys = Arrays.asList(redisKey, localRequestIds.get());
            logger.info("unlock :::: redisKey = " + redisKey + " requestid = " + localRequestIds.get());
            // 使用lua脚本删除redis中匹配value的key，可以避免由于方法执行时间过长而redis锁自动过期失效的时候误删其他线程的锁
            Long result = (Long)redisTemplate.execute(UNLOCK_LUA_SCRIPT, keys);
            //如果这里抛异常，后续锁无法释放
            if (!StringUtils.isEmpty(result) && result == RELEASE_SUCCESS) {
                logger.info("release lock success:" + Thread.currentThread().getName() + ", Status code reply=" + result);
                return true;
            } else if (!StringUtils.isEmpty(result) && result == LOCK_EXPIRED) {
                //返回-1说明获取到的KEY值与requestId不一致或者KEY不存在，可能已经过期或被其他线程加锁
                // 一般发生在key的过期时间短于业务处理时间，属于正常可接受情况
                logger.warn("release lock exception:" + Thread.currentThread().getName() + ", key has expired or released. Status code reply=" + result);
            } else {
                //其他情况，一般是删除KEY失败，返回0
                logger.error("release lock failed:" + Thread.currentThread().getName() + ", del key failed. Status code reply=" + result);
            }
        } catch (Exception e) {
            logger.error("release lock occured an exception", e);
        } finally {
            //清除本地变量
            this.clean();
        }
        return false;
    }

    /**
     * 清除本地线程变量，防止内存泄露
     */
    private void clean() {
        localRequestIds.remove();
        localKeys.remove();
    }
}
