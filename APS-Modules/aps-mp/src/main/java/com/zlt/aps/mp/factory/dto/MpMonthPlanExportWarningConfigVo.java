package com.zlt.aps.mp.factory.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 月计划导出预警配置值
 *
 * @author ZLT
 * @date 20260604
 */
@Data
public class MpMonthPlanExportWarningConfigVo implements Serializable {
    /**
     * 日排产统计下限预警
     */
    private Integer dayMinTotalQty;
    /**
     * 日排产统计上限预警
     */
    private Integer dayMaxTotalQty;
}
