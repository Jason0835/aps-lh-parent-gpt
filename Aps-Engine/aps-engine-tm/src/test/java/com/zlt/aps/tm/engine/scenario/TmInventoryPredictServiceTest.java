package com.zlt.aps.tm.engine.scenario;

import cn.hutool.core.date.DateUtil;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.service.TmInventoryPredictService;
import com.zlt.aps.tm.engine.mapper.TmStockMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 胎面库存预测服务单元测试。
 *
 * <p>验证库存预测逻辑，特别是第一个班的 rollingStockQty 计算：
 * rollingStockQty = 6点库存 - 早班需求量 + 早班计划量</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TmInventoryPredictServiceTest {

    @Mock
    private TmStockMapper tmStockMapper;

    @InjectMocks
    private TmInventoryPredictService inventoryPredictService;

    /**
     * 测试基本库存预测逻辑。
     *
     * <p>场景：6点库存1000米，早班需求量0米，早班计划量0米
     * 预期：rollingStockQty = 1000 - 0 + 0 = 1000米</p>
     */
    @Test
    public void predictShouldCalculateRollingStockQtyCorrectly() {
        // 准备测试数据
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("TR-001", "GL-A", "MP-A");
        context.setTaskDraftList(Arrays.asList(task));

        // 模拟数据库查询结果
        TmStock stock = new TmStock();
        stock.setTreadCode("TR-001");
        stock.setStockQty(new BigDecimal("1000"));
        when(tmStockMapper.selectList(any())).thenReturn(Arrays.asList(stock));

        // 执行库存预测
        inventoryPredictService.predict(context);

        // 验证结果
        Map<String, TmStockForecast> forecastMap = context.getStockForecastMap();
        assertNotNull(forecastMap);
        assertTrue(forecastMap.containsKey("TR-001"));

        TmStockForecast forecast = forecastMap.get("TR-001");
        assertEquals(new BigDecimal("1000"), forecast.getSixClockStockQty());
        assertEquals(new BigDecimal("0"), forecast.getFirstShiftDemandQty());
        assertEquals(new BigDecimal("0"), forecast.getFirstShiftPlanQty());
        assertEquals(new BigDecimal("1000"), forecast.getRollingStockQty());
    }

    /**
     * 测试空任务列表场景。
     */
    @Test
    public void predictShouldReturnEarlyWhenNoTasks() {
        TmScheduleContext context = buildContext();
        context.setTaskDraftList(new ArrayList<>());

        inventoryPredictService.predict(context);

        // 验证没有查询数据库
        verify(tmStockMapper, never()).selectList(any());
    }

    /**
     * 测试空上下文场景。
     */
    @Test(expected = IllegalArgumentException.class)
    public void predictShouldThrowExceptionWhenContextIsNull() {
        inventoryPredictService.predict(null);
    }

    /**
     * 测试空排程日期场景。
     */
    @Test(expected = IllegalArgumentException.class)
    public void predictShouldThrowExceptionWhenScheduleDateIsNull() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F-TM-01");
        context.setOperator("test-user");
        // scheduleDate 为 null

        inventoryPredictService.predict(context);
    }

    private TmScheduleContext buildContext() {
        TmScheduleContext context = new TmScheduleContext();
        context.setFactoryCode("F-TM-01");
        context.setScheduleDate(DateUtil.parseDate("2026-06-15"));
        context.setOperator("test-user");
        return context;
    }

    private TmTaskDraft buildTask(String treadCode, String glueCode, String mouthPlateCode) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo("ORD-TEST-001");
        task.setTreadCode(treadCode);
        task.setGlueCode(glueCode);
        task.setMouthPlateCode(mouthPlateCode);
        return task;
    }
}