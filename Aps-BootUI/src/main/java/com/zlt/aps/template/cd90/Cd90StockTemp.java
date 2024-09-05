package com.zlt.aps.template.cd90;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@ApiModel(value = "90°裁断库存信息导入模板", description = "90°裁断库存信息导入模板")
public class Cd90StockTemp {

    @Excel(name = "ui.data.column.stock.stockDate")
    @ApiModelProperty(value = "库存日期", position = 20)
    private String stockDate;

    @Excel(name = "ui.data.column.loss.clothCode")
    @ApiModelProperty(value = "库存物料编号", position = 30)
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
    private String remark;
}
