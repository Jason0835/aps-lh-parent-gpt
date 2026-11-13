package com.zlt.aps.common.core.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 计划分析报表的动态班次表头。
 */
@Data
public class ScheduleAnalysisShiftHeaderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报表班次序号，范围1至8。
     */
    private Integer classIndex;

    /**
     * 班次对应的实际日期。
     */
    private String shiftDate;

    /**
     * 班次配置编码。
     */
    private String shiftCode;

    /**
     * 班次配置名称或国际化默认名称。
     */
    private String shiftName;

    /**
     * 班次类型。
     */
    private String shiftType;

    /**
     * 班次实际开始时间，无法解析时为空。
     */
    private String shiftStartTime;
}
