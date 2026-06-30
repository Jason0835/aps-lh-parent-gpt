package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tm.service.TmAutoScheduleRedisCacheService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 胎面排程结果缓存清理服务测试。
 *
 * <p>验证排程结果服务层会委托胎面 Redis 缓存服务清理自动排程基础资料缓存。</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TmScheduleResultCacheServiceTest {

    @Mock
    private TmAutoScheduleRedisCacheService tmAutoScheduleRedisCacheService;

    @InjectMocks
    private TmScheduleResultServiceImpl service;

    /**
     * 测试内容：验证服务层清理胎面自动排程 Redis 缓存。
     * 测试场景：指定工厂和排程日期调用清理方法。
     * 预期结果：委托缓存服务清理并返回删除 key 数量。
     */
    @Test
    public void clearAutoPlanRedisCacheShouldDelegateCacheService() {
        Date scheduleDate = DateUtil.parseDate("2026-06-18");
        when(tmAutoScheduleRedisCacheService.clear("F1", scheduleDate)).thenReturn(3L);

        long clearCount = service.clearAutoPlanRedisCache("F1", scheduleDate);

        assertEquals(3L, clearCount);
        verify(tmAutoScheduleRedisCacheService).clear("F1", scheduleDate);
    }
}
