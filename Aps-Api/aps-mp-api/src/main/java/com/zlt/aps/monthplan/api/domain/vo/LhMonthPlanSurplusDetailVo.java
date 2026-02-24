package com.zlt.aps.monthplan.api.domain.vo;

import lombok.Data;

/**
 * 月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Data
public class LhMonthPlanSurplusDetailVo {
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * SAP代码
     */
    private String productCode;
    /**
     * WBS元素
     */
    private String wbsElement;
    /**
     * 开始日期
     */
    private Integer beginDay;
    /**
     * 结束日期
     */
    private Integer endDay;
    /**
     * 计划数量
     */
    private Long planQty;
    /**
     * 本月完成数量
     */
    private Long finishQty;
    /**
     * 剩余数量
     */
    private Long surplusQty;
    /**
     * 内外销标记
     */
    private String locationType;
}
