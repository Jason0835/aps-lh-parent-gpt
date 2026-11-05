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
public interface MachineAssignmentContext<T extends ScheduleTaskDraftModel> extends TwoPhaseScheduleContext {

    /**
     * 获取与生产草稿分离的活动需求消费快照。
     *
     * @return 活动班次到物料消费量，旧入口默认空
     */
    default Map<Integer, Map<String, BigDecimal>> getTwoPhaseConsumptionMap() {
        return java.util.Collections.emptyMap();
    }

    /**
     * 获取领域允许参与自动排程的最大班次序号。
     *
     * @return 最大班次序号
     */
    default int getMaxShiftOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * 获取当日成型总余量额度台账。
     *
     * <p>默认返回空映射，兼容不接入额度的上下文实现。</p>
     *
     * @return 额度桶键到台账的映射
     */
    default Map<String, com.zlt.aps.common.engine.schedule.constraint.ScheduleTailQuotaLedger>
    getTailQuotaLedgerMap() {
        return java.util.Collections.emptyMap();
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
