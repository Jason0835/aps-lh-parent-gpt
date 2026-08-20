package com.zlt.aps.gsq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢丝圈库存管理对象 T_GSQ_STOCK
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GSQ_STOCK")
@ApiModel(value = "钢丝圈库存管理对象", description = "钢丝圈库存管理对象")
public class GsqStock extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 库存日期 */
    @Excel(name = "ui.data.column.gsq.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    @TableField("STOCK_DATE")
    @ImportValidated(required = true, date = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date stockDate;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsq.stock.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码", position = 30)
    @TableField("STEEL_RING_CODE")
    @ImportValidated(required = true, isCode = true, maxLength = 60)
    private String steelRingCode;

    /** 库存量(米) */
    @Excel(name = "ui.data.column.gsq.stock.stockNum", scale = 1)
    @ApiModelProperty(value = "库存量(米)", position = 40)
    @TableField("STOCK_NUM")
    @ImportValidated(required = true, number = true, min = 0, max = 999999, digits = true)
    private BigDecimal stockNum;

    /** 修正数量(米) */
    @Excel(name = "ui.data.column.gsq.stock.modifyNum", scale = 1)
    @ApiModelProperty(value = "修正数量(米)", position = 50)
    @TableField("MODIFY_NUM")
    @ImportValidated(number = true, min = -999999, max = 999999, digits = true)
    private BigDecimal modifyNum;

    /** 不良数量(米) */
    @Excel(name = "ui.data.column.gsq.stock.badNum", scale = 1)
    @ApiModelProperty(value = "不良数量(米)", position = 60)
    @TableField("BAD_NUM")
    @ImportValidated(number = true, min = 0, max = 999999, digits = true)
    private BigDecimal badNum;

    /** 备注（重写以支持Excel导入导出，BaseEntity中的remark无@Excel注解） */
    @Excel(name = "ui.data.column.stock.remark", width = 30)
    @ApiModelProperty(value = "备注", position = 70)
    @TableField("REMARK")
    @ImportValidated(maxLength = 300)
    private String remark;

    /** 库存日期范围-开始（查询用，非数据库字段） */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField(exist = false)
    private Date stockDateStart;

    /** 库存日期范围-结束（查询用，非数据库字段） */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField(exist = false)
    private Date stockDateEnd;
}
