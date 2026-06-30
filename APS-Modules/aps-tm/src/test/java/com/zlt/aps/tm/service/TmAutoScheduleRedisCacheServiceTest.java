package com.zlt.aps.tm.service;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 胎面自动排程 Redis 缓存服务测试。
 *
 * <p>验证基础资料缓存使用 Redis 存储，并支持按工厂和排程日期清理缓存。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TmAutoScheduleRedisCacheServiceTest {

    @Mock
    private RedisService redisService;

    /**
     * 测试内容：验证 Redis 缓存命中时返回集合副本且不回源。
     * 测试场景：Redis 中已经存在机台缓存集合。
     * 预期结果：不会执行 loader，返回值可被调用方修改但不污染缓存对象。
     */
    @Test
    public void getCachedListShouldReturnCopyWhenRedisHit() {
        TmAutoScheduleRedisCacheService cacheService = new TmAutoScheduleRedisCacheService(redisService);
        TmMachineInfo machineInfo = new TmMachineInfo();
        machineInfo.setMachineCode("TM01");
        List<TmMachineInfo> cachedList = new ArrayList<>(Collections.singletonList(machineInfo));
        when(redisService.getCacheObject("aps:tm:autoSchedule:baseData:machine:F1")).thenReturn(cachedList);

        List<TmMachineInfo> resultList = cacheService.getCachedList("machine:F1",
                () -> Collections.singletonList(new TmMachineInfo()));
        resultList.clear();

        assertEquals(1, cachedList.size());
        verify(redisService, never()).setCacheObject(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    /**
     * 测试内容：验证 Redis 未命中时回源并写入空集合缓存。
     * 测试场景：Redis 无参数缓存，loader 返回空集合。
     * 预期结果：loader 只执行一次，并将空集合按 5 分钟 TTL 写入 Redis。
     */
    @Test
    public void getCachedListShouldCacheEmptyListWhenRedisMiss() {
        TmAutoScheduleRedisCacheService cacheService = new TmAutoScheduleRedisCacheService(redisService);
        AtomicInteger loadCount = new AtomicInteger(0);
        when(redisService.getCacheObject("aps:tm:autoSchedule:baseData:params:F1")).thenReturn(null);

        List<String> resultList = cacheService.getCachedList("params:F1", () -> {
            loadCount.incrementAndGet();
            return Collections.emptyList();
        });

        assertEquals(0, resultList.size());
        assertEquals(1, loadCount.get());
        verify(redisService).setCacheObject(eq("aps:tm:autoSchedule:baseData:params:F1"),
                eq(Collections.emptyList()), eq(5L), eq(TimeUnit.MINUTES));
    }

    /**
     * 测试内容：验证指定工厂和日期清理缓存时只删除该工厂基础资料和当日日历。
     * 测试场景：Redis 中存在工厂级参数、机台和多个日期日历缓存。
     * 预期结果：只删除工厂级缓存与指定日期日历缓存，不删除其他日期日历。
     */
    @Test
    public void clearShouldDeleteFactoryAndScheduleDateKeys() {
        TmAutoScheduleRedisCacheService cacheService = new TmAutoScheduleRedisCacheService(redisService);
        Collection<String> matchedKeys = Arrays.asList(
                "aps:tm:autoSchedule:baseData:params:F1",
                "aps:tm:autoSchedule:baseData:machine:F1",
                "aps:tm:autoSchedule:baseData:calendar:F1:03:2026-06-18",
                "aps:tm:autoSchedule:baseData:calendar:F1:04:2026-06-18");
        when(redisService.keys("aps:tm:autoSchedule:baseData:params:F1")).thenReturn(Collections.singletonList("aps:tm:autoSchedule:baseData:params:F1"));
        when(redisService.keys("aps:tm:autoSchedule:baseData:machine:F1")).thenReturn(Collections.singletonList("aps:tm:autoSchedule:baseData:machine:F1"));
        when(redisService.keys("aps:tm:autoSchedule:baseData:calendar:F1:*:2026-06-18")).thenReturn(Arrays.asList(
                "aps:tm:autoSchedule:baseData:calendar:F1:03:2026-06-18",
                "aps:tm:autoSchedule:baseData:calendar:F1:04:2026-06-18"));
        when(redisService.deleteObject(any(Collection.class))).thenReturn(4L);

        long clearCount = cacheService.clear("F1", DateUtil.parseDate("2026-06-18"));

        assertEquals(4L, clearCount);
        ArgumentCaptor<Collection> keyCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisService).deleteObject(keyCaptor.capture());
        assertTrue(keyCaptor.getValue().containsAll(matchedKeys));
        assertEquals(matchedKeys.size(), keyCaptor.getValue().size());
        verify(redisService, never()).keys("aps:tm:autoSchedule:baseData:calendar:F1:*:2026-06-19");
    }

    /**
     * 测试内容：验证全量清理胎面自动排程基础资料缓存。
     * 测试场景：未传工厂和日期。
     * 预期结果：按统一前缀删除所有胎面自动排程基础资料缓存。
     */
    @Test
    public void clearShouldDeleteAllKeysWhenFactoryBlank() {
        TmAutoScheduleRedisCacheService cacheService = new TmAutoScheduleRedisCacheService(redisService);
        Collection<String> matchedKeys = Collections.singletonList("aps:tm:autoSchedule:baseData:params:F1");
        when(redisService.keys("aps:tm:autoSchedule:baseData:*")).thenReturn(matchedKeys);
        when(redisService.deleteObject(any(Collection.class))).thenReturn(1L);

        long clearCount = cacheService.clear(null, null);

        assertEquals(1L, clearCount);
        ArgumentCaptor<Collection> keyCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisService).deleteObject(keyCaptor.capture());
        assertTrue(keyCaptor.getValue().containsAll(matchedKeys));
        assertEquals(matchedKeys.size(), keyCaptor.getValue().size());
    }
}
