package com.zlt.aps.common.engine.schedule.engine;

import java.math.BigDecimal;

/**
 * 自动排程任务的公共排序字段契约。
 *
 * <p>领域任务只暴露排序主流程需要的稳定字段，不包含产品、日志或持久化语义。</p>
 */
public interface ScheduleSortableTask {

    /**
     * 获取计划量计算阶段确定的顺序。
     *
     * @return 计划量顺序
     */
    Integer getPlanCalcOrderIndex();

    /**
     * 获取任务业务键。
     *
     * @return 任务业务键
     */
    String getBusinessKey();

    /**
     * 获取任务班次顺序。
     *
     * @return 班次顺序
     */
    Integer getShiftOrder();

    /**
     * 获取库存供应时长。
     *
     * @return 供应时长
     */
    BigDecimal getSupplyHours();

    /**
     * 写入基础排序序号。
     *
     * @param baseSortIndex 基础排序序号
     */
    void setBaseSortIndex(Integer baseSortIndex);
}

