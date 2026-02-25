package com.zlt.aps.mp.api.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 会计周期对象Vo
 *
 * @author Chen
 * @date 2025/3/21
 */
@Data
public class AccountingPeriodVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年份
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 停车天数
     */
    private Integer stopDay;

    /**
     * 生产计划量
     */
    private BigDecimal produceTotal;

    /**
     * 生产物料数
     */
    private BigDecimal produceSkuCount;

    /**
     * 完成天数
     */
    private Integer finishDateCount;

    /**
     * 完成量
     */
    private BigDecimal finishTotal;

    /**
     * 完成物料数
     */
    private BigDecimal finishSkuCount;


}
