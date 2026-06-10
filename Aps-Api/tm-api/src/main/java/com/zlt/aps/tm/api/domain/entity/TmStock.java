package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@ApiModel(value = "胎面库存对象", description = "胎面库存对象")
@Data
@TableName(value = "T_TM_STOCK")
public class TmStock extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.stock.factoryCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.stock.stockDate")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "库存日期", name = "stockDate")
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    @Excel(name = "ui.data.column.tm.stock.treadCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "胎面编码", name = "treadCode")
    @TableField(value = "TREAD_CODE")
    private String treadCode;

    @Excel(name = "ui.data.column.tm.stock.stockQty")
    @ImportValidated(number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "库存数量", name = "stockQty")
    @TableField(value = "STOCK_QTY")
    private BigDecimal stockQty;

    @Excel(name = "ui.data.column.tm.stock.badQty")
    @ImportValidated(number = true, min = 0, max = 999999)
    @ApiModelProperty(value = "不良数量", name = "badQty")
    @TableField(value = "BAD_QTY")
    private BigDecimal badQty;

    @Excel(name = "ui.data.column.tm.stock.adjustQty")
    @ImportValidated(number = true, min = -999999, max = 999999)
    @ApiModelProperty(value = "调整数量", name = "adjustQty")
    @TableField(value = "ADJUST_QTY")
    private BigDecimal adjustQty;
}
