package com.zlt.aps.template.cd15;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.Date;

@ApiModel(value = "15°裁断库存导入模板", description = "15°裁断库存导入模板")
public class Cd15StockTemp  {


    @Excel(name = "ui.data.column.stock.stockDate")
    @ApiModelProperty(value = "库存日期", position = 20)
    private String stockDate;

    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.common.column.gy.steelStripCode")
    private String materialCode;

    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum")
    private BigDecimal stockNum;

    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    private BigDecimal modifyNum;

    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.badNum")
    private BigDecimal badNum;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(maxLength = 300)
    private String remark;
}
