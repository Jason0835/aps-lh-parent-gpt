package com.zlt.aps.tc.service.impl;

import com.zlt.aps.tc.mapper.TcParamsMapper;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcStockMapper;
import com.zlt.aps.tc.service.TcBackgroundTaskService;
import com.zlt.aps.tc.service.loader.TcAutoScheduleDataLoadService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

/**
 * 胎侧自动滚动库存上下界计算测试。
 */
public class TcAutoRollingAsyncExecutorImplTest {

    private TcAutoRollingAsyncExecutorImpl executor;

    /**
     * 创建不访问外部服务的滚动执行器。
     */
    @Before
    public void setUp() {
        this.executor = new TcAutoRollingAsyncExecutorImpl(
                Mockito.mock(TcBackgroundTaskService.class),
                Mockito.mock(TcAutoScheduleDataLoadService.class),
                Mockito.mock(TcScheduleResultMapper.class),
                Mockito.mock(TcStockMapper.class),
                Mockito.mock(TcParamsMapper.class),
                Mockito.mock(TcManualOperationFacade.class));
    }

    /**
     * 库存与原计划低于一班需求时，应补足到一班需求下界。
     */
    @Test
    public void shouldIncreasePlanToOneShiftDemandLowerBound() {
        BigDecimal desiredQty = this.executor.calculateDesiredPlanQty(
                new BigDecimal("200"), new BigDecimal("300"),
                new BigDecimal("800"), 3);

        Assert.assertEquals(0, new BigDecimal("600").compareTo(desiredQty));
    }

    /**
     * 库存与原计划超过三班需求时，应下调到配置的库存上界。
     */
    @Test
    public void shouldReducePlanToConfiguredStockUpperBound() {
        BigDecimal desiredQty = this.executor.calculateDesiredPlanQty(
                new BigDecimal("1000"), new BigDecimal("1800"),
                new BigDecimal("800"), 3);

        Assert.assertEquals(0, new BigDecimal("1400").compareTo(desiredQty));
    }

    /**
     * 库存落在上下界内时，应保持原计划量不变。
     */
    @Test
    public void shouldKeepPlanWhenStockWithinBounds() {
        BigDecimal desiredQty = this.executor.calculateDesiredPlanQty(
                new BigDecimal("700"), new BigDecimal("500"),
                new BigDecimal("800"), 3);

        Assert.assertEquals(0, new BigDecimal("500").compareTo(desiredQty));
    }
}
