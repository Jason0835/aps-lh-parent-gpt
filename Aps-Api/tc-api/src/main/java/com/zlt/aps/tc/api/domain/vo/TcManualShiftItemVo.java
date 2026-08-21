package com.zlt.aps.tc.api.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 胎侧人工操作班次明细。
 */
@Data
@ApiModel(value = "胎侧人工操作班次明细")
public class TcManualShiftItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次顺序，范围 1 到 6。 */
    @ApiModelProperty(value = "班次顺序", required = true)
    private Integer shiftOrder;

    /** 计划量，单位米。 */
    @ApiModelProperty(value = "计划量", required = true)
    private BigDecimal planQty;

    /** 班内顺序。 */
    @ApiModelProperty(value = "班内顺序", required = true)
    private Integer sequence;

    /** 班次原因分析。 */
    @ApiModelProperty(value = "班次原因分析")
    private String analysis;
}
