package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_tq_stock")
@ApiModel(value = "胎圈库存信息对象", description = "胎圈库存信息对象")
public class TqStock extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    @TableField("STOCK_DATE")
    @ImportValidated(required = true, date = true)
    private Date stockDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "查询库存的开始日期", position = 21)
    @TableField(exist = false)
    private Date stockDateStart;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ApiModelProperty(value = "查询库存的结束日期", position = 22)
    @TableField(exist = false)
    private Date stockDateEnd;

    @ApiModelProperty(value = "胎圈编码", position = 30)
    @Excel(name = "ui.data.column.tq.scheduleResult.beadCode")
    @TableField("BEAD_CODE")
    @ImportValidated(required = true, maxLength = 50, isCode = true)
    private String beadCode;

    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum", scale = 1)
    @TableField("STOCK_NUM")
    @ImportValidated(required = true, number = true, min = 0, max = 999999, digits = true)
    private BigDecimal stockNum;

    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    @TableField("MODIFY_NUM")
    @ImportValidated(number = true, min = -999999, max = 999999, digits = true)
    private BigDecimal modifyNum;

    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.badNum")
    @TableField("BAD_NUM")
    @ImportValidated(number = true, min = 0, max = 999999, digits = true)
    private BigDecimal badNum;

    @Excel(name = "ui.data.column.stock.remark")
    @TableField("REMARK")
    @ImportValidated(maxLength = 300)
    private String remark;
}
