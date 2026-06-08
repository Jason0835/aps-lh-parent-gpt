package com.zlt.aps.mp.factory.dto;

import com.ruoyi.common.core.web.domain.AjaxResult;

import lombok.Data;

/**
 * 结构转产表导入helper
 *
 */
@Data
public class MpStructureAllocationImportHelper {
    /**
     * 年份
     */
    private Integer year;
    /**
     * 月份
     */
    private Integer month;
    /**
     * 结构转产表导入参数
     */
    private String[] params;
    /**
     * 月计划导入参数
     */
    private String[] params4DayResult;
    /**
     * 需求计划版本号
     */
    private String monthPlanVersion;
    /**
     * 生产计划版本号
     */
    private String productVersion;
    /**
     * 校验结果
     */
    private AjaxResult ajaxResult;
}
