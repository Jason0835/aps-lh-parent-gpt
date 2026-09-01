package com.zlt.aps.common.core.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 人工调量批次中的班次修改明细。
 */
@Data
@ApiModel(value = "人工调量班次修改明细")
public class ChangeQtyTaskVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待修改班次。
     */
    @ApiModelProperty(value = "待修改班次", required = true)
    private Integer shiftOrder;

    /**
     * 修改后的计划量。
     */
    @ApiModelProperty(value = "修改后的计划量", required = true)
    private BigDecimal newPlanQty;

    /**
     * 修改后的原因分析。
     */
    @ApiModelProperty(value = "修改后的原因分析")
    private String newAnalysis;
}
