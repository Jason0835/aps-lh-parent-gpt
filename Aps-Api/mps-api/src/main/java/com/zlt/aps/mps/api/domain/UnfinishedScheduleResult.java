package com.zlt.aps.mps.api.domain;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

/**
 * 未完成排程记录
 * @author zlt
 *
 */
@Data
public class UnfinishedScheduleResult {
    /**
     * 排产日期
     */
    private Date scheduleDate;
    /**
     * 工序
     */
    private String processes;
    /**
     * 规格
     */
    private String matrialCode;
    /**
     * 机台
     */
    private String machineName;
    /**
     * 早班计划
     */
    private BigDecimal dayPlanQty;
    /**
     * 早班完成
     */
    private BigDecimal dayFinishQty;
    /**
     * 早班完成率
     */
    private String dayFinishRate;
    /**
     * 早班原因分析
     */
    private String dayHandAnalysis;
    /**
     * 夜班计划
     */
    private BigDecimal nightPlanQty;
    /**
     * 夜班完成
     */
    private BigDecimal nightFinishQty;
    /**
     * 夜班完成率
     */
    private String nightFinishRate;
    /**
     * 夜班原因分析
     */
    private String nightHandAnalysis;
}
