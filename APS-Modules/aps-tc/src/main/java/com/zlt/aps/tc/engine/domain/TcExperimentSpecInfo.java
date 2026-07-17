package com.zlt.aps.tc.engine.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 胎侧实验规格识别与计划量证据。
 *
 * <p>用于记录月计划定稿实验规格转换为胎侧排程任务的依据，并在计划量计算阶段写入规则命中证据。</p>
 */
@Data
public class TcExperimentSpecInfo {

    /** 是否命中实验规格规则 */
    private Boolean experimentSpec;

    /** 实验规格回看天数 */
    private Integer lookbackDays;

    /** 回看天数参数来源 */
    private String lookbackDaysSource;

    /** 实验规格固定计划量 */
    private BigDecimal planQty;

    /** 固定计划量参数来源 */
    private String planQtySource;

    /** 胎侧排程日期 */
    private Date scheduleDate;

    /** 月计划定稿生产日期 */
    private Date experimentPlanDate;

    /** 月计划日数量合计 */
    private BigDecimal monthPlanDayQty;

    /** 月计划定稿主键集合 */
    private List<Long> monthPlanIds;

    /** 月计划定稿工单号集合 */
    private List<String> productionNos;

    /** 胎胚号集合 */
    private List<String> embryoCodes;

    /** 是否叠加到已有同胎侧任务 */
    private Boolean mergedToExistingTask;

    /** 判断说明 */
    private String reason;

    /**
     * 判断当前证据是否命中实验规格。
     *
     * @return true 表示命中实验规格规则
     */
    public boolean isExperimentSpecHit() {
        return Boolean.TRUE.equals(experimentSpec);
    }
}
