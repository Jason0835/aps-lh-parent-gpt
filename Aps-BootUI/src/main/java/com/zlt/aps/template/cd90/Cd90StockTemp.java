package com.zlt.aps.template.cd90;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "90°裁断库存信息导入模板", description = "90°裁断库存信息导入模板")
public class Cd90StockTemp extends BaseEntity {

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

    /**
     * 库存量(卷)
     */
    @ApiModelProperty(value = "库存量(卷)", position = 70)
    @Excel(name = "ui.data.column.stock.rollStockNum", scale = 1)
    @ImportValidated(name = "ui.data.column.stock.rollStockNum", number = true, min = 0, max = 999999)
    private BigDecimal rollStockNum;

    /**
     * 修正数量(卷)
     */
    @ApiModelProperty(value = "修正数量(卷)", position = 80)
    @Excel(name = "ui.data.column.stock.rollModifyNum")
    @ImportValidated(name = "ui.data.column.stock.rollModifyNum", number = true, min = -999999, max = 999999)
    private BigDecimal rollModifyNum;

    /**
     * 不良数量(卷)
     */
    @ApiModelProperty(value = "不良数量(卷)", position = 90)
    @Excel(name = "ui.data.column.stock.rollBadNum")
    @ImportValidated(name = "ui.data.column.stock.rollBadNum", number = true, min = 0, max = 999999)
    private BigDecimal rollBadNum;

    @Excel(name = "ui.data.column.stock.remark")
    private String remark;
}
