package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 计划量公共引擎上下文契约。
 *
 * @param <T> 任务类型
 * @param <F> 库存预测类型
 */
public interface PlanCalculationContext<T extends ScheduleTaskDraftModel,
        F extends ScheduleInventoryForecast, G extends SchedulePlanTaskGroup<T>> {

    List<T> getTaskDraftList();

    void setTaskDraftList(List<T> taskDraftList);

    List<T> getSourceTaskDraftList();

    void setSourceTaskDraftList(List<T> sourceTaskDraftList);

    Map<String, G> getPlanTaskGroupMap();

    void setPlanTaskGroupMap(Map<String, G> planTaskGroupMap);

    String getFactoryCode();

    Date getScheduleDate();

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
}
