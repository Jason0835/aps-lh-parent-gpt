package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.MachineShiftTaskChain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 机台分配公共引擎上下文访问端口。
 *
 * @param <T> 任务类型
 */
public interface MachineAssignmentContext<T extends ScheduleTaskDraftModel> {

    /**
     * 获取领域允许参与自动排程的最大班次序号。
     *
     * @return 最大班次序号
     */
    default int getMaxShiftOrder() {
        return Integer.MAX_VALUE;
    }

    List<T> getTaskDraftList();

    BigDecimal getCurrentAvailableToolQty();

    void setCurrentAvailableToolQty(BigDecimal quantity);

    BigDecimal getInitialAvailableToolQty();

    Map<String, BigDecimal> getInitialStockMap();

    MachineShiftTaskChain<T> getTaskChainGroup();

    Map<String, BigDecimal> getProductShiftShortageMap();

    void setProductShiftShortageMap(Map<String, BigDecimal> shortageMap);

    void setRemainingStockMap(Map<String, BigDecimal> remainingStockMap);
}
