package com.zlt.aps.monthplan.api.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Chen
 * @date 2025/3/14
 */
@Data
public class SkuSummaryTrialProductVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 月份，如果是13=年累计、14=H1、15=环比
     */
    @ApiModelProperty(value = "月份，如果是13=年累计、14=H1、15=环比", name = "month")
    private Integer month;

    /**
     * 物料号
     */
    @ApiModelProperty(value = "物料号", name = "productCode")
    private String productCode;

    /**
     * 生产天数
     */
    @ApiModelProperty(value = "生产天数", name = "finishDay")
    private BigDecimal finishDay;

    /**
     * 生产条数
     */
    @ApiModelProperty(value = "生产条数", name = "finishSum")
    private BigDecimal finishSum;

}
