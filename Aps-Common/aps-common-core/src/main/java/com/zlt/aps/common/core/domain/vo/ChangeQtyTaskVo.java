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
     * 目标顺序；已有节点表示移动到该位置，未传时保持原位置；空班次新增时为空表示追加到当前机台该班次末尾。
     */
    @ApiModelProperty(value = "调量目标顺序，已有节点移动或空班次新增时使用，TM和TC共用")
    private Integer newSequence;

    /**
     * 修改后的原因分析。
     */
    @ApiModelProperty(value = "修改后的原因分析")
    private String newAnalysis;
}
