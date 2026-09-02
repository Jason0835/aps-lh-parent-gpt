package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TM/TC 自动排程计划量汇总组公共非持久化模型。
 *
 * @param <T> 领域任务草稿类型
 */
@Data
public class SchedulePlanTaskGroup<T extends ScheduleTaskDraftModel>
        implements ScheduleQualityPlanGroup<T> {

    /** 汇总组业务键 */
    protected String planGroupKey;

    /** 汇总生产任务 */
    protected T aggregateTask;

    /** 原始来源任务快照 */
    protected List<T> sourceTaskList = new ArrayList<>();

    /** 来源任务分摊权重，key=来源任务业务键 */
    protected Map<String, BigDecimal> sourceWeightMap = new LinkedHashMap<>();

    /** 汇总当前班需求量 */
    protected BigDecimal groupCurrentShiftDemandQty;

    /** 汇总下一排程班需求量 */
    protected BigDecimal groupNextShiftDemandQty;

    /** 汇总保证范围需求量 */
    protected BigDecimal groupGuardDemandQty;

    /** 汇总库存抵扣后基础需求量 */
    protected BigDecimal groupBaseDemandQty;

    /** 汇总最小起排调整量 */
    protected BigDecimal groupMinStartAdjustQty;

    /** 汇总收尾或卷曲取整调整量 */
    protected BigDecimal groupRoundAdjustQty;

    /** 汇总最终计划量 */
    protected BigDecimal groupFinalPlanQty;
}

