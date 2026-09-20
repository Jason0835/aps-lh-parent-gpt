package com.zlt.aps.common.engine.schedule.engine;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
/** TM/TC 实验规格识别公共运行态模型。 */
@Data
public class ScheduleExperimentSpecInfoModel {
    /** 是否命中实验规格。 */
    protected Boolean experimentSpec;
    /**
     * 成型计划施工阶段。
     */
    protected String constructionStage;
    /**
     * 成型计划产品状态。
     */
    protected String productStatus;
    /**
     * 兼容旧解释字段；当前实验规格判断不读取月计划回看天数。
     */
    protected Integer lookbackDays;
    /** 兼容旧解释字段来源；当前实验规格判断不读取月计划回看天数。 */
    protected String lookbackDaysSource;
    /** 实验规格提前生产班数。 */
    protected Integer advanceShiftCount;
    /** 实验规格提前生产班数来源。 */
    protected String advanceShiftCountSource;
    /** 基础备库班数。 */
    protected Integer baseGuardShiftCount;
    /** 取最大值后的有效备库班数。 */
    protected Integer effectiveGuardShiftCount;
    /** 备库窗口起始成型班次。 */
    protected Integer formingWindowStartClass;
    /** 备库窗口结束成型班次。 */
    protected Integer formingWindowEndClass;
    /** 超过 CLASS8 后需要按末三班平均量估算的班次数。 */
    protected Integer formingWindowEstimatedShiftCount;
    /** 排程日期。 */
    protected LocalDate scheduleDate;
    /** 兼容旧解释字段；当前实验规格判断不读取实验月计划日期。 */
    protected LocalDate experimentPlanDate;
    /** 兼容旧解释字段；当前实验规格判断不读取月计划数量。 */
    protected BigDecimal monthPlanDayQty;
    /** 兼容旧解释字段；当前实验规格判断不读取月计划主键。 */
    protected List<Long> monthPlanIds;
    /** 兼容旧解释字段；当前实验规格判断不读取月计划工单号。 */
    protected List<String> productionNos;
    /** 兼容旧解释字段；当前实验规格判断不读取月计划胎胚号。 */
    protected List<String> embryoCodes;
    /** 命中原因说明。 */
    protected String reason;

    /**
     * 判断当前模型是否命中实验规格。
     *
     * @return true 表示命中实验规格
     */
    public boolean isExperimentSpecHit() {
        return Boolean.TRUE.equals(experimentSpec);
    }
}
