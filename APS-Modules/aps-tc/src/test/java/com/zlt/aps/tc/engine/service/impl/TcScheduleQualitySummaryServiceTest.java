package com.zlt.aps.tc.engine.service.impl;

import com.zlt.aps.common.engine.schedule.ScheduleOperationContext;
import com.zlt.aps.common.engine.schedule.ScheduleTaskLinkedList;
import com.zlt.aps.common.engine.schedule.ScheduleTaskNode;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.engine.domain.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * 胎侧自动排程质量指标口径测试。
 */
public class TcScheduleQualitySummaryServiceTest {

    /**
     * 验证利用率按米数与班产定额计算，切换次数按实际任务链切换计算。
     */
    @Test
    public void shouldBuildQualitySummaryFromTaskChainFacts() {
        TcScheduleContext context = this.buildContext();
        TcPersistResult persistResult = new TcPersistResult();
        persistResult.setResultCount(2);
        persistResult.setUnplannedCount(0);

        Map<String, Object> summary = new TcScheduleQualitySummaryService().build(context, persistResult);

        assertEquals(0, BigDecimal.valueOf(0.5D)
                .compareTo(BigDecimal.valueOf((Double) summary.get("machineUtilizationRate"))));
        assertEquals(1L, summary.get("switchCount"));
        assertEquals(0, BigDecimal.valueOf(0.5D)
                .compareTo(BigDecimal.valueOf((Double) summary.get("stockGuaranteeRate"))));
        assertEquals(0, BigDecimal.valueOf(0.5D)
                .compareTo(BigDecimal.valueOf((Double) summary.get("shiftCapacityHitRate"))));
    }

    /**
     * 构造包含两条机台班次链和库存结果的测试上下文。
     *
     * @return 排程运行上下文
     */
    private TcScheduleContext buildContext() {
        TcScheduleContext context = new TcScheduleContext();
        context.setFactoryCode("116");
        context.setScheduleDate(java.sql.Date.valueOf(LocalDate.of(2026, 7, 14)));
        TcParamValue capacityParam = new TcParamValue();
        capacityParam.setParamCode(TcScheduleConstants.PARAM_SHIFT_MAX_CAPACITY);
        capacityParam.setParamValue(TcScheduleConstants.DEFAULT_SHIFT_MAX_CAPACITY);
        context.getParamMap().put(TcScheduleConstants.PARAM_SHIFT_MAX_CAPACITY, capacityParam);

        TcTaskDraft firstTask = this.buildTask("SW-QUALITY-1", "TC01", BigDecimal.valueOf(3000));
        firstTask.setPreviousSpecSwitchHours(BigDecimal.valueOf(0.5));
        firstTask.setTailFlag("1");
        TcTaskDraft secondTask = this.buildTask("SW-QUALITY-2", "TC02", BigDecimal.valueOf(2500));
        secondTask.setBusinessKeySuffix(TcScheduleConstants.CAPACITY_OVERFLOW_BUSINESS_KEY_PREFIX + "QUALITY");
        context.setTaskDraftList(Arrays.asList(firstTask, secondTask));
        this.appendTask(context, firstTask);
        this.appendTask(context, secondTask);

        context.getStockForecastMap().put("SW-QUALITY-1", new TcStockForecast());
        context.getStockForecastMap().put("SW-QUALITY-2", new TcStockForecast());
        context.getRemainingStockMap().put("SW-QUALITY-1", BigDecimal.ONE);
        context.getRemainingStockMap().put("SW-QUALITY-2", BigDecimal.ONE.negate());
        return context;
    }

    /**
     * 构造已分配任务。
     *
     * @param sidewallCode 胎侧编码
     * @param machineCode 机台编码
     * @param planQty 计划量
     * @return 任务草稿
     */
    private TcTaskDraft buildTask(String sidewallCode, String machineCode, BigDecimal planQty) {
        TcTaskDraft task = new TcTaskDraft();
        task.setSidewallCode(sidewallCode);
        task.setMachineCode(machineCode);
        task.setShiftOrder(1);
        task.setPlanQty(planQty);
        return task;
    }

    /**
     * 将任务加入对应机台班次链。
     *
     * @param context 排程运行上下文
     * @param task 已分配任务
     */
    private void appendTask(TcScheduleContext context, TcTaskDraft task) {
        ScheduleTaskLinkedList<TcTaskDraft> chain = context.getTaskChainGroup()
                .getOrCreate(task.getMachineCode(), LocalDate.of(2026, 7, 14), task.getShiftOrder());
        ScheduleTaskNode<TcTaskDraft> node = new ScheduleTaskNode<>(task.getBusinessKey(), task,
                task.getMachineCode(), LocalDate.of(2026, 7, 14), "CLASS1", task.getShiftOrder(), task.getPlanQty());
        chain.append(node, new ScheduleOperationContext("tester", "AUTO_APPEND", "TRACE-QUALITY"));
    }
}
