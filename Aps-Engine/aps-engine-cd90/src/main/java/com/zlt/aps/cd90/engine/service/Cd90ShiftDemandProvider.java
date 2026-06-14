package com.zlt.aps.cd90.engine.service;

import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90RollingScheduleContext;
import com.zlt.aps.cd90.engine.model.Cd90ScheduleCandidate;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDemandDecision;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;

import java.math.BigDecimal;

/**
 * 多班执行器的需求计算边界。
 *
 * <p>停复产、预计库存、后续有效计划量和月计划余量由该边界统一提供，执行器不直接查询业务表。</p>
 */
public interface Cd90ShiftDemandProvider {

    /**
     * 取得当前班次单个候选规格的净需求和月计划余量。
     */
    Cd90ShiftDemandDecision resolve(Cd90AutoScheduleContext context,
                                    Cd90AutoScheduleInput input,
                                    Cd90ShiftDescriptor shift,
                                    Cd90ScheduleCandidate candidate,
                                    Cd90RollingScheduleContext rolling);

    /**
     * 取得6点至当前班次开始前的累计成型消耗量。
     */
    default BigDecimal cumulativeConsumptionBeforeShift(Cd90AutoScheduleContext context,
                                                        Cd90AutoScheduleInput input,
                                                        Cd90ShiftDescriptor shift) {
        return BigDecimal.ZERO;
    }
}
