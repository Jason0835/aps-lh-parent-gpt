package com.zlt.aps.template.gsq;

import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "钢丝圈库存信息导入模板", description = "钢丝圈库存信息导入模板")
public class GsqStockTemp {

    @Excel(name = "ui.data.column.stock.stockDate", width = 30)
    @ApiModelProperty(value = "库存日期", position = 20)
    private String stockDate;

    @ApiModelProperty(value = "钢丝圈代码", position = 30)
    @Excel(name = "ui.data.column.quota.steelRingCode")
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
