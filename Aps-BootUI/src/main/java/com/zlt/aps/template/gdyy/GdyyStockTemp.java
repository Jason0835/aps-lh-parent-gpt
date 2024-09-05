package com.zlt.aps.template.gdyy;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "钢带压延库存信息对象", description = "钢带压延库存信息对象")
public class GdyyStockTemp {

    @Excel(name = "ui.data.column.stock.stockDate")
    @ApiModelProperty(value = "库存日期", position = 20)
    private String stockDate;

    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.common.column.gy.bigRollCode")
    private String materialCode;

    @ApiModelProperty(value = "大卷库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockRollNum")
    private BigDecimal stockRollNum;

    @ApiModelProperty(value = "MES库存量", position = 40)
    @Excel(name = "ui.data.column.gdyy.stock.stockNum")
    private BigDecimal stockNum;

    @ApiModelProperty(value = "MES库存修正数量", position = 50)
    @Excel(name = "ui.data.column.gdyy.stock.modifyNum")
    private BigDecimal modifyNum;

    @ApiModelProperty(value = "MES库存不良数量", position = 60)
    @Excel(name = "ui.data.column.gdyy.stock.badNum")
    private BigDecimal badNum;

    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
