package com.zlt.aps.template.xwyy;

import java.math.BigDecimal;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "纤维压延预生产库存倍数设定对象", description = "纤维压延预生产库存倍数设定对象 ")
public class XwyyReserveStockTemp extends BaseEntity {

    @Excel(name = "ui.data.column.xwyy.quota.bigRollCode", sort = 10)
    @ApiModelProperty(value = "纤维大卷编号")
    private String bigRollCode;

    /** 预生产库存倍数 */
    @Excel(name = "ui.data.column.xwyy.reserveStock.reserveStockRate", sort = 20)
    @ApiModelProperty(value = "预生产库存倍数")
    private BigDecimal reserveStockRate;

    @Excel(name = "ui.common.column.remark", sort = 30)
    @ApiModelProperty(value = "备注", position = 500)
    private String remark;
}
