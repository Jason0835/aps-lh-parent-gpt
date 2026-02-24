package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhScheduleResultTotalVo {
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * SAP代码
     */
    private String productCode;
    /**
     * 规格编码
     */
    private String specCode;

    // 日完成量
    private Integer totalFinishQty;

    // 日计划量
    private Integer totalPlanQty;

    private Date realScheduleDate;
}
