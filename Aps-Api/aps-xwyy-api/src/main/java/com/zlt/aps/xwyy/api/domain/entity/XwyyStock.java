package com.zlt.aps.xwyy.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@ApiModel(value = "纤维压延库存", description = "纤维压延库存")
@TableName("t_xwyy_stock")
public class XwyyStock extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("工厂编码")
    @ImportValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.xwyyStock.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    @ApiModelProperty("库存日期")
    @ImportValidated(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("STOCK_DATE")
    @Excel(name = "ui.data.column.xwyyStock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date stockDate;

    @ApiModelProperty("帘线大卷编号")
    @ImportValidated(required = true, maxLength = 30)
    @TableField("BIG_ROLL_CODE")
    @Excel(name = "ui.data.column.xwyyStock.bigRollCode")
    private String bigRollCode;

    @ApiModelProperty("大卷条码")
    @ImportValidated(maxLength = 50)
    @TableField("BIG_ROLL_BARCODE")
    @Excel(name = "ui.data.column.xwyyStock.bigRollBarcode")
    private String bigRollBarcode;

    @ApiModelProperty("库存量（个）")
    @ImportValidated(required = true, number = true, min = 0, max = 999999)
    @TableField("STOCK_NUM")
    @Excel(name = "ui.data.column.xwyyStock.stockNum")
    private BigDecimal stockNum;

    @ApiModelProperty("库存量（卷）")
    @ImportValidated(number = true, min = 0, max = 999999)
    @TableField("STOCK_ROLL_NUM")
    @Excel(name = "ui.data.column.xwyyStock.stockRollNum")
    private BigDecimal stockRollNum;

    @ApiModelProperty("修正数量")
    @ImportValidated(number = true, min = 0, max = 999999)
    @TableField("MODIFY_NUM")
    @Excel(name = "ui.data.column.xwyyStock.modifyNum")
    private BigDecimal modifyNum;

    @ApiModelProperty("修正数量（卷）")
    @ImportValidated(number = true, min = 0, max = 999999)
    @TableField("ROLL_MODIFY_NUM")
    @Excel(name = "ui.data.column.xwyyStock.rollModifyNum")
    private BigDecimal rollModifyNum;

    @ApiModelProperty("不良数量")
    @ImportValidated(number = true, min = 0, max = 999999)
    @TableField("BAD_NUM")
    @Excel(name = "ui.data.column.xwyyStock.badNum")
    private BigDecimal badNum;

    @ApiModelProperty("不良数量（卷）")
    @ImportValidated(number = true, min = 0, max = 999999)
    @TableField("ROLL_BAD_NUM")
    @Excel(name = "ui.data.column.xwyyStock.rollBadNum")
    private BigDecimal rollBadNum;

    @ApiModelProperty("预计库存标记")
    @TableField("ESTIMATE_STOCK_FLAG")
    @Excel(name = "ui.data.column.xwyyStock.estimateStockFlag")
    private String estimateStockFlag;
}