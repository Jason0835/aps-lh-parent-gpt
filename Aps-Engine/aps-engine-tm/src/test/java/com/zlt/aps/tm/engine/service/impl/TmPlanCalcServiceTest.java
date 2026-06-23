package com.zlt.aps.tm.engine.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmStockForecast;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.engine.strategy.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 胎面计划计算服务测试。
 *
 * <p>验证需求量算法参数只以 TM_ALGORITHM_SWITCH 为准，避免旧参数影响需求量算法选择。</p>
 */
public class TmPlanCalcServiceTest {

    /**
     * 测试内容：验证计划计算服务读取新算法开关参数。
     * 测试场景：上下文参数中存在 `TM_ALGORITHM_SWITCH=2`。
     * 预期结果：读取到算法编码 `2`，后续需求计算按新开关控制。
     */
    @Test
    public void readAlgorithmCodeShouldUseTmAlgorithmSwitch() {
        TmPlanCalcService service = buildService();
        TmScheduleContext context = new TmScheduleContext();
        context.getParamMap().put("TM_ALGORITHM_SWITCH", buildParam("TM_ALGORITHM_SWITCH", "2"));

        assertEquals("2", service.readAlgorithmCode(context));
    }

    /**
     * 测试内容：验证算法开关缺失时使用默认算法。
     * 测试场景：上下文参数 map 为空。
     * 预期结果：读取结果为默认值 `1`，避免参数缺失导致排程中断。
     */
    @Test
    public void readAlgorithmCodeShouldUseDefaultWhenSwitchMissing() {
        TmPlanCalcService service = buildService();
        TmScheduleContext context = new TmScheduleContext();

        assertEquals("1", service.readAlgorithmCode(context));
    }

    /**
     * 测试内容：验证计划计算不再读取遗留需求算法参数。
     * 测试场景：只配置旧参数 `DEMAND_QTY_CALCULATE_TYPE=2`，不配置新参数。
     * 预期结果：读取结果仍为默认值 `1`，旧参数不影响当前算法选择。
     */
    @Test
    public void readAlgorithmCodeShouldIgnoreLegacyDemandQtyCalculateType() {
        TmPlanCalcService service = buildService();
        TmScheduleContext context = new TmScheduleContext();
        context.getParamMap().put("DEMAND_QTY_CALCULATE_TYPE", buildParam("DEMAND_QTY_CALCULATE_TYPE", "2"));

        assertEquals("1", service.readAlgorithmCode(context));
    }

    /**
     * 测试内容：验证计划计算上下文为空时直接拒绝。
     * 测试场景：调用计划计算服务时传入 null。
     * 预期结果：抛出业务异常，避免后续读取参数和任务时报空指针。
     */
    @Test(expected = ServiceException.class)
    public void calculateShouldRejectNullContext() {
        buildRealService().calculate(null);
    }

    /**
     * 测试内容：验证任务列表为空时直接返回。
     * 测试场景：上下文无待排任务，策略注册表为空。
     * 预期结果：不读取策略、不抛异常，任务列表仍为空。
     */
    @Test
    public void calculateShouldReturnWhenTaskListEmpty() {
        TmPlanCalcService service = buildService();
        TmScheduleContext context = new TmScheduleContext();

        service.calculate(context);

        assertTrue(context.getTaskDraftList().isEmpty());
    }

    /**
     * 测试内容：验证库存预测会回填到任务，且已有计划量不被覆盖。
     * 测试场景：任务已有 planQty，上下文存在同胎面库存预测。
     * 预期结果：rollingStockQty 和 sixClockStockQty 使用预测值，需求重新计算，原计划量保持不变。
     */
    @Test
    public void calculateShouldBackfillForecastAndKeepExistingPlanQty() {
        TmPlanCalcService service = buildRealService();
        TmScheduleContext context = new TmScheduleContext();
        TmTaskDraft task = buildTask("ORD-KEEP", "TR-KEEP");
        task.setDemandQty(new BigDecimal("100"));
        task.setGuardDemandQty(new BigDecimal("500"));
        task.setGuardRangeHours(new BigDecimal("8"));
        task.setPlanQty(new BigDecimal("777"));
        context.setTaskDraftList(Collections.singletonList(task));
        TmStockForecast forecast = new TmStockForecast();
        forecast.setTreadCode("TR-KEEP");
        forecast.setSixClockStockQty(new BigDecimal("1000"));
        forecast.setRollingStockQty(new BigDecimal("200"));
        context.getStockForecastMap().put("TR-KEEP", forecast);

        service.calculate(context);

        assertEquals(new BigDecimal("1000"), task.getSixClockStockQty());
        assertEquals(new BigDecimal("200"), task.getRollingStockQty());
        assertEquals(new BigDecimal("300"), task.getDemandQty());
        assertEquals(new BigDecimal("777"), task.getPlanQty());
        assertTrue(context.getRuleTraceMap().get(task.getBusinessKey()).toExplainJson().contains("DEMAND_QTY_CALC"));
    }

    /**
     * 测试内容：验证计划量为空时按默认计划量策略计算。
     * 测试场景：当前班需求 120，未维护 planQty。
     * 预期结果：服务会调用默认计划量策略并回填计划量。
     */
    @Test
    public void calculateShouldFillPlanQtyWhenMissing() {
        TmPlanCalcService service = buildRealService();
        TmScheduleContext context = new TmScheduleContext();
        TmTaskDraft task = buildTask("ORD-CALC", "TR-CALC");
        task.setCurrentShiftDemandQty(new BigDecimal("120"));
        task.setGuardDemandQty(new BigDecimal("120"));
        task.setRollingStockQty(BigDecimal.ZERO);
        context.setTaskDraftList(Collections.singletonList(task));

        service.calculate(context);

        assertEquals(new BigDecimal("120"), task.getDemandQty());
        assertEquals(new BigDecimal("120"), task.getPlanQty());
        assertTrue(context.getRuleTraceMap().get(task.getBusinessKey()).toExplainJson().contains("PLAN_QTY_CALC"));
    }

    /**
     * 测试内容：验证收尾余量大于基础需求时不按收尾量覆盖计划量。
     * 测试场景：MARK_CLOSE_OUT_TIP 已折算为收尾标识，但收尾余量乘胎面肩长大于基础需求。
     * 预期结果：计划量保持基础需求量，不被更大的收尾余量放大。
     */
    @Test
    public void calculateShouldSkipTailQtyWhenTailBaseGreaterThanBaseDemand() {
        TmPlanCalcService service = buildRealService();
        TmScheduleContext context = new TmScheduleContext();
        TmTaskDraft task = buildTask("ORD-TAIL", "TR-TAIL");
        task.setCurrentShiftDemandQty(new BigDecimal("500"));
        task.setGuardDemandQty(new BigDecimal("500"));
        task.setRollingStockQty(BigDecimal.ZERO);
        task.setTailFlag("1");
        task.setTailBalanceQty(new BigDecimal("100"));
        task.setTreadShoulderLength(new BigDecimal("10"));
        context.setTaskDraftList(Collections.singletonList(task));

        service.calculate(context);

        assertEquals(new BigDecimal("500"), task.getBaseDemandQty());
        assertEquals(new BigDecimal("500"), task.getPlanQty());
    }

    /**
     * 构建计划计算服务，当前测试只覆盖参数读取，不需要注册实际策略。
     *
     * @return 计划计算服务
     */
    private TmPlanCalcService buildService() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        return new TmPlanCalcService(registry);
    }

    /**
     * 构建包含默认需求和计划策略的计划计算服务。
     *
     * @return 计划计算服务
     */
    private TmPlanCalcService buildRealService() {
        TmStrategyRegistry registry = new TmStrategyRegistry(Collections.singletonList(new TmGuardDemandQtyStrategy()),
                Collections.singletonList(new TmDefaultPlanQtyStrategy()),
                Collections.singletonList(new TmDefaultMachineFilterRule()),
                Collections.singletonList(new TmDefaultMachineScoreStrategy()),
                Collections.singletonList(new TmDefaultTaskSortStrategy()));
        return new TmPlanCalcService(registry);
    }

    /**
     * 构建待排任务草稿。
     *
     * @param orderNo   订单号
     * @param treadCode 胎面编码
     * @return 待排任务草稿
     */
    private TmTaskDraft buildTask(String orderNo, String treadCode) {
        TmTaskDraft task = new TmTaskDraft();
        task.setOrderNo(orderNo);
        task.setTreadCode(treadCode);
        task.setGlueCode("GL-" + orderNo);
        task.setMouthPlateCode("MP-" + orderNo);
        return task;
    }

    /**
     * 构建排程参数快照值。
     *
     * @param paramCode  参数编码
     * @param paramValue 参数值
     * @return 参数快照值
     */
    private TmParamValue buildParam(String paramCode, String paramValue) {
        TmParamValue value = new TmParamValue();
        value.setParamCode(paramCode);
        value.setParamValue(paramValue);
        value.setSource("TEST");
        return value;
    }
}
