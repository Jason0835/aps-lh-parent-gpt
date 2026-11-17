package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
/** TM/TC 新规格识别公共运行态模型。 */
@Data
public class ScheduleNewSpecInfoModel {
    /**
     * 是否命中新规格判定。
     */
    protected Boolean newSpec;
    /** 历史排程与库存回溯天数。 */
    protected Integer lookbackDays;
    /** 回溯天数参数的来源（如系统参数编码或默认兜底）。 */
    protected String lookbackDaysSource;
    /** 新规格提前生产（扩展备库窗口）班次数。 */
    protected Integer advanceShiftCount;
    /** 提前生产班次参数的来源（如系统参数编码或默认兜底）。 */
    protected String advanceShiftCountSource;
    /** 基础库存保证班数。 */
    protected Integer baseGuardShiftCount;
    /** 结合新规格与实验规格提前班数后最终生效的保证班数。 */
    protected Integer effectiveGuardShiftCount;
    /** 成型需求窗口起始班次（对应成型 CLASS 序号）。 */
    protected Integer formingWindowStartClass;
    /** 成型需求窗口结束班次（对应成型 CLASS 序号）。 */
    protected Integer formingWindowEndClass;
    /** 成型需求窗口内通过末三班均值外推估算的班次数。 */
    protected Integer formingWindowEstimatedShiftCount;
    /** 校验使用的前一日库存日期。 */
    protected LocalDate previousStockDate;
    /** 前一日库存数量（米）。 */
    protected BigDecimal previousDayStockQty;
    /** 是否存在前一日库存记录。 */
    protected Boolean previousDayStockExists;
    /** 历史排程计划校验窗口开始日期。 */
    protected LocalDate historyStartDate;
    /** 历史排程计划校验窗口结束日期。 */
    protected LocalDate historyEndDate;
    /** 回溯窗口内是否存在历史排程计划记录。 */
    protected Boolean historySchedulePlanExists;
    /** 原计划目标排程班次。 */
    protected Integer normalTargetShift;
    /** 调整后的目标排程班次。 */
    protected Integer adjustedTargetShift;
    /** 调整后的目标排程班次窗口列表。 */
    protected List<Integer> adjustedTargetWindow;
    /** 对应成型需求班次。 */
    protected Integer demandShift;
    /** 对应需求数量（米）。 */
    protected BigDecimal demandQty;
    /** 新规格判定结论及原因说明。 */
    protected String reason;

    /**
     * 判断当前规格是否命中新规格规则。
     *
     * @return 命中返回 true，否则返回 false
     */
    public boolean isNewSpecHit() {
        return Boolean.TRUE.equals(newSpec);
    }
}
