package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎侧同代码同班次计划量汇总组。
 *
 * <p>保留原始成型来源任务，同时承载进入后续排程链路的唯一汇总生产任务。</p>
 */
@Data
public class TcPlanTaskGroup {

    /** 汇总组业务键 */
    private String planGroupKey;

    /** 汇总生产任务 */
    private TcTaskDraft aggregateTask;

    /** 原始来源任务快照 */
    private List<TcTaskDraft> sourceTaskList = new ArrayList<>();

    /** 来源任务分摊权重，key=来源任务业务键 */
    private Map<String, BigDecimal> sourceWeightMap = new LinkedHashMap<>();

    /** 汇总当前班需求量 */
    private BigDecimal groupCurrentShiftDemandQty;

    /** 汇总下一排程班需求量 */
    private BigDecimal groupNextShiftDemandQty;

    /** 汇总保证范围需求量 */
    private BigDecimal groupGuardDemandQty;

    /** 汇总库存抵扣后基础需求量 */
    private BigDecimal groupBaseDemandQty;

    /** 汇总最小起排调整量 */
    private BigDecimal groupMinStartAdjustQty;

    /** 汇总收尾或卷曲取整调整量 */
    private BigDecimal groupRoundAdjustQty;

    /** 汇总最终计划量 */
    private BigDecimal groupFinalPlanQty;
}
