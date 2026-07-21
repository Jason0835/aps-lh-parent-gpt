package com.zlt.aps.cd15.engine.service;

import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15RollingScheduleContext;
import com.zlt.aps.cd15.engine.model.Cd15ScheduleCandidate;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDemandDecision;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 多班执行器的需求计算边界。
 *
 * <p>停复产、预计库存、后续有效计划量和月计划余量由该边界统一提供，执行器不直接查询业务表。</p>
 */
public interface Cd15ShiftDemandProvider {

    /**
     * 取得当前班次单个候选规格的净需求和月计划余量。
     */
    Cd15ShiftDemandDecision resolve(Cd15AutoScheduleContext context,
                                    Cd15AutoScheduleInput input,
                                    Cd15ShiftDescriptor shift,
                                    Cd15ScheduleCandidate candidate,
                                    Cd15RollingScheduleContext rolling);

    /**
     * 取得6点至当前班次开始前的累计成型消耗量，按钢带代号分组。
     */
    default Map<String, BigDecimal> cumulativeConsumptionBySteelStripBeforeShift(
            Cd15AutoScheduleContext context, Cd15AutoScheduleInput input, Cd15ShiftDescriptor shift) {
        return Collections.emptyMap();
    }

    /**
     * 兼容旧调用：返回所有钢带累计成型消耗合计值。
     */
    default BigDecimal cumulativeConsumptionBeforeShift(Cd15AutoScheduleContext context,
                                                        Cd15AutoScheduleInput input,
                                                        Cd15ShiftDescriptor shift) {
        return cumulativeConsumptionBySteelStripBeforeShift(context, input, shift).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}