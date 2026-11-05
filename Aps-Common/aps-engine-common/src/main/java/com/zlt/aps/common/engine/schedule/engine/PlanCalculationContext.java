package com.zlt.aps.common.engine.schedule.engine;

import com.zlt.aps.common.engine.schedule.constraint.ScheduleTailQuotaLedger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 计划量公共引擎上下文契约。
 *
 * @param <T> 任务类型
 * @param <F> 库存预测类型
 */
public interface PlanCalculationContext<T extends ScheduleTaskDraftModel,
        F extends ScheduleInventoryForecast, G extends SchedulePlanTaskGroup<T>> extends TwoPhaseScheduleContext {

    List<T> getTaskDraftList();

    void setTaskDraftList(List<T> taskDraftList);

    List<T> getSourceTaskDraftList();

    void setSourceTaskDraftList(List<T> sourceTaskDraftList);

    Map<String, G> getPlanTaskGroupMap();

    void setPlanTaskGroupMap(Map<String, G> planTaskGroupMap);

    String getFactoryCode();

    LocalDate getScheduleDate();

    Map<String, F> getStockForecastMap();

    /**
     * 获取计划量计算过程中的运行库存。
     *
     * @return 按工序编码汇总的运行库存
     */
    Map<String, BigDecimal> getRemainingStockMap();

    void setInitialStockMap(Map<String, BigDecimal> initialStockMap);

    void setProductShiftShortageMap(Map<String, BigDecimal> shortageMap);

    void setRemainingStockMap(Map<String, BigDecimal> remainingStockMap);

    void setInitialAvailableToolQty(BigDecimal quantity);

    /**
     * 获取当前可用工装数量。
     *
     * @return 当前可用工装数量
     */
    BigDecimal getCurrentAvailableToolQty();

    void setCurrentAvailableToolQty(BigDecimal quantity);

    Map<Integer, BigDecimal> getShiftHoursMap();

    /**
     * 获取当日成型总余量额度台账。
     *
     * <p>默认返回空映射，兼容不接入额度的上下文实现。</p>
     *
     * @return 额度桶键到台账的映射
     */
    default Map<String, ScheduleTailQuotaLedger> getTailQuotaLedgerMap() {
        return java.util.Collections.emptyMap();
    }

    /**
     * 设置当日成型总余量额度台账。
     *
     * @param ledgerMap 额度桶键到台账的映射
     */
    default void setTailQuotaLedgerMap(Map<String, ScheduleTailQuotaLedger> ledgerMap) {
        // 默认不支持额度台账。
    }

    /**
     * 判断成型总余量台账是否已初始化。
     *
     * @return 已初始化时返回 true
     */
    default boolean isTailQuotaPrepared() {
        return false;
    }

    /**
     * 设置成型总余量台账初始化标记。
     *
     * @param prepared 初始化标记
     */
    default void setTailQuotaPrepared(boolean prepared) {
        // 默认不支持额度台账。
    }
}
