package com.zlt.aps.mp.api.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Sandy
 * @version 1.0
 * @Description 周程滚动调整-增模 上下文对象
 * @date 2025/12/19
 */
@Data
public class IncMouldContext implements Serializable {

    private static final long serialVersionUID = 8736122348031246578L;

    @ApiModelProperty(value = "首次增模标识")
    private boolean bFirstAddMould = true;

    @ApiModelProperty(value = "按硫化机维度，记录前日排产量")
    private Integer beforeProductionQty = 0;

    @ApiModelProperty(value = "按硫化机维度，记录前日排产量位置")
    private Integer beforeProductionPosition = 0;

    @ApiModelProperty(value = "按硫化机维度，记录已排产天数")
    private Integer usedProductionDays = 0;

    @ApiModelProperty(value = "已排产计划量")
    private Integer hasProductionQty = 0;
}
