package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.engine.algorithm.Cd15NewSpecAdvanceResolver;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleParameters;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15NewSpecAdvanceResult;
import com.zlt.aps.cd15.engine.service.impl.Cd15NewSpecAdvanceInputPreparer;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 新增规格历史排程计划量输入准备测试。 */
public class Cd15NewSpecAdvanceInputPreparerTest {

    /** 验证历史任一班次计划量大于0时即视为已经排过。 */
    @Test
    public void shouldTreatPositivePlanQuantityAsScheduled() {
        Cd15ScheduleResult history = this.history("C01", 100D, 0D);
        Cd15NewSpecAdvanceInputPreparer preparer = new Cd15NewSpecAdvanceInputPreparer(
                this.mapper(Collections.singletonList(history)),
                new Cd15NewSpecAdvanceResolver());

        Cd15NewSpecAdvanceResult result = preparer.prepare(this.context(), this.input());

        assertFalse(result.getAdvanceInfoBySteelStrip().containsKey("C01"));
    }

    /** 验证历史计划量全为0时，即使存在完成量也不作为已经排过的依据。 */
    @Test
    public void shouldAdvanceWhenAllPlanQuantitiesAreZero() {
        Cd15ScheduleResult history = this.history("C01", 0D, 1D);
        Cd15NewSpecAdvanceInputPreparer preparer = new Cd15NewSpecAdvanceInputPreparer(
                this.mapper(Collections.singletonList(history)),
                new Cd15NewSpecAdvanceResolver());

        Cd15NewSpecAdvanceResult result = preparer.prepare(this.context(), this.input());

        assertTrue(result.getAdvanceInfoBySteelStrip().containsKey("C01"));
    }

    /** 使用动态代理构造固定历史结果的Mapper，测试不依赖Mockito。 */
    private Cd15EngineScheduleResultMapper mapper(List<Cd15ScheduleResult> historyResults) {
        return (Cd15EngineScheduleResultMapper) java.lang.reflect.Proxy.newProxyInstance(
                Cd15EngineScheduleResultMapper.class.getClassLoader(),
                new Class<?>[]{Cd15EngineScheduleResultMapper.class},
                (proxy, method, arguments) -> "selectList".equals(method.getName())
                        ? historyResults : null);
    }

    /** 构建历史排程结果。 */
    private Cd15ScheduleResult history(String steelStripCode, Double planQuantity,
                                       Double finishQuantity) {
        Cd15ScheduleResult result = new Cd15ScheduleResult();
        result.setFactoryCode("116");
        result.setSteelStripCode(steelStripCode);
        result.setClass1PlanQty(planQuantity);
        result.setClass8FinishQty(finishQuantity);
        return result;
    }

    /** 构建参数和排程日期上下文。 */
    private Cd15AutoScheduleContext context() {
        return Cd15AutoScheduleContext.builder()
                .factoryCode("116")
                .scheduleDate(LocalDate.of(2026, 7, 9))
                .parameters(Cd15AutoScheduleParameters.builder()
                        .newSpecLookbackDays(10)
                        .newSpecAdvanceDays(2)
                        .build())
                .build();
    }

    /** 构建未来成型需求。 */
    private Cd15AutoScheduleInput input() {
        Cd15DemandShift demand = Cd15DemandShift.builder()
                .steelStripCode("C01").classField("CLASS1").shiftKey("D1")
                .startTime(LocalDateTime.of(2026, 7, 9, 6, 0))
                .steelStripDemandQuantity(new BigDecimal("100"))
                .included(true).build();
        return Cd15AutoScheduleInput.builder()
                .demandShifts(Collections.singletonList(demand))
                .build();
    }
}