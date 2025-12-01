package com.zlt.aps.template.gsq;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "钢丝圈预生产库存倍数设定对象", description = "钢丝圈预生产库存倍数设定对象 ")
public class GsqReserveStockTemp extends BaseEntity {

    @Excel(name = "ui.data.column.gsq.reserveStock.steelRingCode", sort = 10)
    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    /** 预生产库存倍数 */
    @Excel(name = "ui.data.column.gsq.reserveStock.reserveStockRate", sort = 20)
    @ApiModelProperty(value = "预生产库存倍数")
    private BigDecimal reserveStockRate;

    @Excel(name = "ui.common.column.remark", sort = 30)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
