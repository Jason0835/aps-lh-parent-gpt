package com.zlt.aps.cx.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 调量请求VO
 *
 * @author APS Team
 */
@Data
@ApiModel(value = "调量请求对象")
public class ScheduleAdjustVo {

    @ApiModelProperty(value = "排程记录ID", required = true)
    private Long id;

    @ApiModelProperty(value = "夜班计划量")
    private BigDecimal class1PlanQty;

    @ApiModelProperty(value = "早班计划量")
    private BigDecimal class2PlanQty;

    @ApiModelProperty(value = "中班计划量")
    private BigDecimal class3PlanQty;

    @ApiModelProperty(value = "第4班计划量")
    private BigDecimal class4PlanQty;

    @ApiModelProperty(value = "第5班计划量")
    private BigDecimal class5PlanQty;

    @ApiModelProperty(value = "第6班计划量")
    private BigDecimal class6PlanQty;

    @ApiModelProperty(value = "第7班计划量")
    private BigDecimal class7PlanQty;

    @ApiModelProperty(value = "第8班计划量")
    private BigDecimal class8PlanQty;

    @ApiModelProperty(value = "夜班完成量（校验用）")
    private BigDecimal class1FinishQty;

    @ApiModelProperty(value = "早班完成量（校验用）")
    private BigDecimal class2FinishQty;

    @ApiModelProperty(value = "中班完成量（校验用）")
    private BigDecimal class3FinishQty;
}
