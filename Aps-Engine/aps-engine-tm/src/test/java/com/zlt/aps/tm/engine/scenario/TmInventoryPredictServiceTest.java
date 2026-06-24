package com.zlt.aps.tm.engine.scenario;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.api.domain.entity.TmStock;
import com.zlt.aps.tm.engine.domain.TmInventoryPredictQtyVo;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.mapper.TmEngineInventoryPredictMapper;
import com.zlt.aps.tm.engine.mapper.TmEngineStockMapper;
import com.zlt.aps.tm.engine.service.impl.TmInventoryPredictService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
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
    private TmEngineStockMapper tmStockMapper;

    @Mock
    private TmEngineInventoryPredictMapper tmEngineInventoryPredictMapper;

    @InjectMocks
    private TmInventoryPredictService inventoryPredictService;

    /**
     * 测试内容：验证基础库存预测公式。
     * 测试场景：6 点库存 1000 米，早班需求量 0 米，早班计划量 0 米。
     * 预期结果：rollingStockQty = 1000 - 0 + 0 = 1000 米，并写入库存预测 map。
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
        when(tmEngineInventoryPredictMapper.selectFirstShiftDemandRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(tmEngineInventoryPredictMapper.selectFirstShiftPlanRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());

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
     * 测试内容：验证库存预测按胎面编码过滤早班需求和上一夜班计划。
     * 测试场景：上下文只有胎面 TR-001，mapper 返回同胎面早班需求 300 和上一夜班计划 120。
     * 预期结果：预测字段只回填 TR-001，滚动库存为 1000 - 300 + 120 = 820，并按胎面列表查询 mapper。
     */
    @Test
    public void predictShouldUseFirstShiftDemandAndPreviousNightPlanWithTreadFilter() {
        TmScheduleContext context = buildContext();
        TmTaskDraft task = buildTask("TR-001", "GL-A", "MP-A");
        context.setTaskDraftList(Collections.singletonList(task));
        TmStock stock = new TmStock();
        stock.setTreadCode("TR-001");
        stock.setStockQty(new BigDecimal("1000"));
        when(tmStockMapper.selectList(any())).thenReturn(Collections.singletonList(stock));
        TmInventoryPredictQtyVo demandRow = new TmInventoryPredictQtyVo();
        demandRow.setTreadCode("TR-001");
        demandRow.setQty(new BigDecimal("300"));
        TmInventoryPredictQtyVo planRow = new TmInventoryPredictQtyVo();
        planRow.setTreadCode("TR-001");
        planRow.setQty(new BigDecimal("120"));
        when(tmEngineInventoryPredictMapper.selectFirstShiftDemandRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(demandRow));
        when(tmEngineInventoryPredictMapper.selectFirstShiftPlanRows(any(), any(), any()))
                .thenReturn(Collections.singletonList(planRow));

        inventoryPredictService.predict(context);

        TmStockForecast forecast = context.getStockForecastMap().get("TR-001");
        assertEquals(new BigDecimal("300"), forecast.getFirstShiftDemandQty());
        assertEquals(new BigDecimal("120"), forecast.getFirstShiftPlanQty());
        assertEquals(new BigDecimal("820"), forecast.getRollingStockQty());
        verify(tmEngineInventoryPredictMapper).selectFirstShiftDemandRows(
                eq("F-TM-01"), eq(context.getScheduleDate()), eq(Collections.singletonList("TR-001")));
        verify(tmEngineInventoryPredictMapper).selectFirstShiftPlanRows(
                eq("F-TM-01"), eq(DateUtil.offsetDay(context.getScheduleDate(), -1)), eq(Collections.singletonList("TR-001")));
    }

    /**
     * 测试内容：验证 6 点库存会扣减不良量和调整量。
     * 测试场景：库存 1000，不良 50，调整 30。
     * 预期结果：参与滚动预测的 6 点库存为 920。
     */
    @Test
    public void predictShouldSubtractBadQtyAndAdjustQtyFromSixClockStock() {
        TmScheduleContext context = buildContext();
        context.setTaskDraftList(Collections.singletonList(buildTask("TR-001", "GL-A", "MP-A")));
        TmStock stock = new TmStock();
        stock.setTreadCode("TR-001");
        stock.setStockQty(new BigDecimal("1000"));
        stock.setBadQty(new BigDecimal("50"));
        stock.setAdjustQty(new BigDecimal("30"));
        when(tmStockMapper.selectList(any())).thenReturn(Collections.singletonList(stock));
        when(tmEngineInventoryPredictMapper.selectFirstShiftDemandRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(tmEngineInventoryPredictMapper.selectFirstShiftPlanRows(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        inventoryPredictService.predict(context);

        TmStockForecast forecast = context.getStockForecastMap().get("TR-001");
        assertEquals(new BigDecimal("920"), forecast.getSixClockStockQty());
        assertEquals(new BigDecimal("920"), forecast.getRollingStockQty());
    }

    /**
     * 测试内容：验证早班需求和早班计划查询异常时按 0 兜底。
     * 测试场景：6 点库存无记录，两个明细查询均抛出运行时异常。
     * 预期结果：库存、早班需求、早班计划和滚动库存均为 0，不中断排程。
     */
    @Test
    public void predictShouldFallbackToZeroWhenDemandAndPlanQueriesFail() {
        TmScheduleContext context = buildContext();
        context.setTaskDraftList(Collections.singletonList(buildTask("TR-ERR", "GL-A", "MP-A")));
        when(tmStockMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(tmEngineInventoryPredictMapper.selectFirstShiftDemandRows(any(), any(), any()))
                .thenThrow(new RuntimeException("demand query failed"));
        when(tmEngineInventoryPredictMapper.selectFirstShiftPlanRows(any(), any(), any()))
                .thenThrow(new RuntimeException("plan query failed"));

        inventoryPredictService.predict(context);

        TmStockForecast forecast = context.getStockForecastMap().get("TR-ERR");
        assertEquals(BigDecimal.ZERO, forecast.getSixClockStockQty());
        assertEquals(BigDecimal.ZERO, forecast.getFirstShiftDemandQty());
        assertEquals(BigDecimal.ZERO, forecast.getFirstShiftPlanQty());
        assertEquals(BigDecimal.ZERO, forecast.getRollingStockQty());
    }

    /**
     * 测试内容：验证没有待排任务时跳过库存预测。
     * 测试场景：上下文合法，但 taskDraftList 为空列表。
     * 预期结果：直接返回，不查询库存和早班需求计划数据。
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
     * 测试内容：验证库存预测服务拒绝空上下文。
     * 测试场景：调用 predict(null)。
     * 预期结果：抛出 ServiceException，避免后续读取工厂和日期时报空指针。
     */
    @Test(expected = ServiceException.class)
    public void predictShouldThrowExceptionWhenContextIsNull() {
        inventoryPredictService.predict(null);
    }

    /**
     * 测试内容：验证库存预测服务拒绝空排程日期。
     * 测试场景：上下文包含工厂和操作人，但 scheduleDate 为空。
     * 预期结果：抛出 ServiceException，不继续查询库存和需求计划。
     */
    @Test(expected = ServiceException.class)
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
