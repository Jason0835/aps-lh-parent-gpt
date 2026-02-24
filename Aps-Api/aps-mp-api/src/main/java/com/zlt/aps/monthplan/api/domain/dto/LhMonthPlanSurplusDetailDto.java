package com.zlt.aps.monthplan.api.domain.dto;

import lombok.Data;

/**
 * 查询月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhMonthPlanSurplusDetailDto {
    /**
     * token
     */
    private String token;
    /**
     * 年
     */
    private Integer year;
    /**
     * 月
     */
    private Integer month;
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * SAP代码
     */
    private String productCode;
    /**
     * 页码
     */
    private Integer pageNum;
    /**
     * 条数
     */
    private Integer pageSize;

}
