package com.zlt.aps.lh.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 硫化日计划调整需求汇总。
 */
@Data
@ApiModel(value = "硫化日计划调整需求汇总")
public class LhDayPlanAdjustRequireSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 调整1合计 */
    @ApiModelProperty("调整1合计")
    private BigDecimal adjustQty1 = BigDecimal.ZERO;

    /** 调整2合计 */
    @ApiModelProperty("调整2合计")
    private BigDecimal adjustQty2 = BigDecimal.ZERO;

    /** 调整3合计 */
    @ApiModelProperty("调整3合计")
    private BigDecimal adjustQty3 = BigDecimal.ZERO;

    /** 调整后总合计 */
    @ApiModelProperty("调整后总合计")
    private BigDecimal adjustedTotalQty = BigDecimal.ZERO;
}
