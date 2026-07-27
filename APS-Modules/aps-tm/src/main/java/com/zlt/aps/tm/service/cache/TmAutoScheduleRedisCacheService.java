package com.zlt.aps.tm.service.cache;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Supplier;

/**
 * 胎面自动排程 Redis 基础资料缓存服务。
 *
 * <p>该服务只缓存跨请求复用的自动排程基础资料，运行态排程上下文仍由单次请求内存对象承载。</p>
 */
@Slf4j
@Service
public class TmAutoScheduleRedisCacheService {

    @Resource
    private RedisService redisService;

    /**
     * 创建缓存服务。
     */
    public TmAutoScheduleRedisCacheService() {
    }

    /**
     * 创建缓存服务，供单元测试注入 Redis 依赖。
     *
     * @param redisService Redis 服务
     */
    public TmAutoScheduleRedisCacheService(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 读取带短 TTL 的 Redis 集合缓存。
     *
     * @param cacheKeySuffix 缓存 key 后缀
     * @param loader         缓存未命中时的数据加载器
     * @param <T>            集合元素类型
     * @return 集合副本
     */
    public <T> List<T> getCachedList(String cacheKeySuffix, Supplier<List<T>> loader) {
        // 暂不使用 Redis 缓存：每次直接回源加载，确保参数等基础资料修改后立即生效。
        // 保留方法签名与 clear() 清理入口，便于后续按需恢复缓存。
        return copyList(loader.get());
    }

    /**
     * 清理胎面自动排程 Redis 基础资料缓存。
     *
     * @param factoryCode  工厂编码，为空时清理全部胎面自动排程基础资料缓存
     * @param scheduleDate 排程日期，和工厂编码同时传入时只清理该日期工作日历及该工厂级基础资料缓存
     * @return 实际删除的 key 数量
     */
    public long clear(String factoryCode, Date scheduleDate) {
        if (redisService == null) {
            return 0L;
        }
        Set<String> deleteKeySet = new LinkedHashSet<>();
        if (StrUtil.isBlank(factoryCode)) {
            deleteKeySet.addAll(findKeys(TmScheduleConstants.BASE_DATA_CACHE_KEY_PREFIX + "*"));
        } else {
            deleteKeySet.addAll(findKeys(TmScheduleConstants.BASE_DATA_CACHE_KEY_PREFIX + "params:" + factoryCode));
            // 同时清理旧版和包含停用机台证据的 v2 机台缓存，避免管理端清理后仍命中旧数据。
            deleteKeySet.addAll(findKeys(TmScheduleConstants.BASE_DATA_CACHE_KEY_PREFIX + "machine:" + factoryCode));
            deleteKeySet.addAll(findKeys(TmScheduleConstants.BASE_DATA_CACHE_KEY_PREFIX + "machine:v2:" + factoryCode));
            if (scheduleDate == null) {
                deleteKeySet.addAll(findKeys(TmScheduleConstants.BASE_DATA_CACHE_KEY_PREFIX
                        + "calendar:" + factoryCode + ":*"));
            } else {
                deleteKeySet.addAll(findKeys(TmScheduleConstants.BASE_DATA_CACHE_KEY_PREFIX
                        + "calendar:" + factoryCode + ":*:" + DateUtil.formatDate(scheduleDate)));
            }
        }
        if (deleteKeySet.isEmpty()) {
            return 0L;
        }
        return redisService.deleteObject(deleteKeySet);
    }

    /**
     * 查询匹配的 Redis key。
     *
     * @param pattern Redis key 匹配表达式
     * @return 匹配到的 key 集合
     */
    private Collection<String> findKeys(String pattern) {
        try {
            Collection<String> keyCollection = redisService.keys(pattern);
            return keyCollection == null ? Collections.emptyList() : keyCollection;
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_CACHE] 查询 Redis 缓存 key 失败，pattern={}，原因={}", pattern, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 复制集合，避免调用方修改缓存对象本身。
     *
     * @param sourceList 原始集合
     * @param <T>        集合元素类型
     * @return 集合副本
     */
    private <T> List<T> copyList(List<T> sourceList) {
        if (sourceList == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(sourceList);
    }
}
