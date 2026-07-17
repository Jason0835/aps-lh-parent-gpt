package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎侧排程看板汇总。
 */
@Data
@ApiModel(value = "胎侧排程看板汇总")
public class TcScheduleBoardSummaryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划总量。 */
    @ApiModelProperty(value = "计划总量")
    private BigDecimal totalPlanQty = BigDecimal.ZERO;

    /** 完成总量。 */
    @ApiModelProperty(value = "完成总量")
    private BigDecimal totalFinishQty = BigDecimal.ZERO;

    /** 已排结果数。 */
    @ApiModelProperty(value = "已排结果数")
    private Long resultCount = 0L;
}
