package com.zlt.aps.tc.service.cache;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 胎侧自动排程 Redis 基础资料缓存测试。
 */
@RunWith(MockitoJUnitRunner.class)
public class TcAutoScheduleRedisCacheServiceTest {

    @Mock
    private RedisService redisService;

    /**
     * 验证缓存命中时返回副本且不访问数据库回源函数。
     */
    @Test
    public void shouldReturnCopyWithoutFallbackWhenRedisHit() {
        TcAutoScheduleRedisCacheService cacheService = new TcAutoScheduleRedisCacheService(this.redisService);
        TcMachineInfo machineInfo = new TcMachineInfo();
        machineInfo.setMachineCode("TC01");
        List<TcMachineInfo> cachedList = Collections.singletonList(machineInfo);
        when(this.redisService.getCacheObject("aps:tc:autoSchedule:baseData:machine:F1"))
                .thenReturn(cachedList);

        List<TcMachineInfo> resultList = cacheService.getCachedList("machine:F1",
                () -> Collections.singletonList(new TcMachineInfo()));
        resultList.clear();

        assertEquals(1, cachedList.size());
        verify(this.redisService, never()).setCacheObject(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    /**
     * 验证缓存未命中时只回源一次，并按五分钟 TTL 缓存空集合。
     */
    @Test
    public void shouldFallbackAndCacheEmptyListForFiveMinutes() {
        TcAutoScheduleRedisCacheService cacheService = new TcAutoScheduleRedisCacheService(this.redisService);
        AtomicInteger loadCount = new AtomicInteger();
        when(this.redisService.getCacheObject("aps:tc:autoSchedule:baseData:params:F1")).thenReturn(null);

        List<String> resultList = cacheService.getCachedList("params:F1", () -> {
            loadCount.incrementAndGet();
            return Collections.emptyList();
        });

        assertTrue(resultList.isEmpty());
        assertEquals(1, loadCount.get());
        verify(this.redisService).setCacheObject(eq("aps:tc:autoSchedule:baseData:params:F1"),
                eq(Collections.emptyList()), eq(5L), eq(TimeUnit.MINUTES));
    }

    /**
     * 验证按工厂和日期只清理目标参数、机台和工作日历缓存。
     */
    @Test
    public void shouldClearFactoryAndScheduleDateCacheKeys() {
        TcAutoScheduleRedisCacheService cacheService = new TcAutoScheduleRedisCacheService(this.redisService);
        when(this.redisService.keys("aps:tc:autoSchedule:baseData:params:F1"))
                .thenReturn(Collections.singletonList("aps:tc:autoSchedule:baseData:params:F1"));
        when(this.redisService.keys("aps:tc:autoSchedule:baseData:machine:F1"))
                .thenReturn(Collections.singletonList("aps:tc:autoSchedule:baseData:machine:F1"));
        when(this.redisService.keys("aps:tc:autoSchedule:baseData:calendar:F1:*:2026-07-14"))
                .thenReturn(Arrays.asList("aps:tc:autoSchedule:baseData:calendar:F1:03:2026-07-14",
                        "aps:tc:autoSchedule:baseData:calendar:F1:04:2026-07-14"));
        when(this.redisService.deleteObject(any(Collection.class))).thenReturn(4L);

        long clearCount = cacheService.clear("F1", DateUtil.parseDate("2026-07-14"));

        assertEquals(4L, clearCount);
        ArgumentCaptor<Collection> keyCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(this.redisService).deleteObject(keyCaptor.capture());
        assertEquals(4, keyCaptor.getValue().size());
    }
}
