package com.zlt.aps.tq.api.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 胎圈自动滚动调量指令。
 *
 * <p>对齐胎面 TmRollingAdjustment，承载 calculateAdjustments 算法的输出，
 * 描述对单条 TqScheduleResult 的目标班次计划量调整意图。</p>
 *
 * @author APS
 */
@Data
@ApiModel(value = "胎圈自动滚动调量指令", description = "库存上下界触发调量的目标值和证据")
public class TqRollingAdjustment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 调整方向：UP 调增，DOWN 调减。 */
    @ApiModelProperty(value = "调整方向（UP/DOWN）")
    private String direction;

    /** 胎圈编码。 */
    @ApiModelProperty(value = "胎圈编码")
    private String beadCode;

    /** 调整前目标班计划量。 */
    @ApiModelProperty(value = "调整前目标班计划量")
    private BigDecimal beforePlanQty;

    /** 调整后目标班计划量。 */
    @ApiModelProperty(value = "调整后目标班计划量")
    private BigDecimal targetPlanQty;

    /** 调整量（targetPlanQty - beforePlanQty）。 */
    @ApiModelProperty(value = "调整量")
    private BigDecimal deltaQty;

    /** 证据链：库存、需求、阈值等关键计算值。 */
    @ApiModelProperty(value = "证据链")
    private Map<String, Object> evidence = new LinkedHashMap<>();
}
