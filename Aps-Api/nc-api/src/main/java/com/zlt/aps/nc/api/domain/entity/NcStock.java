package com.zlt.aps.nc.api.domain.entity;

import java.math.BigDecimal;
import java.util.Date;

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

/**
 * 内衬库存信息对象
 *
 * @author zlt
 * @date 2026-05-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "内衬库存信息对象", description = "内衬库存信息对象")
@TableName("T_NC_STOCK")
public class NcStock extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ImportValidated(name = "ui.data.column.stock.stockDate", required = true, date = true)
    @ApiModelProperty(value = "库存日期", position = 20)
    @TableField("STOCK_DATE")
    private Date stockDate;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd", position = 21)
    @TableField(exist = false)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd", position = 22)
    @TableField(exist = false)
    private String endTime;

    /**
     * 库存物料编号
     */
    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.quota.liningCode")
    @ImportValidated(name = "ui.data.column.quota.liningCode", required = true, maxLength = 50, isCode = true)
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum")
    @ImportValidated(name = "ui.data.column.stock.stockNum", number = true, min = 0, max = 999999)
    @TableField("STOCK_NUM")
    private BigDecimal stockNum;

    /**
     * 修正数量
     */
    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    @ImportValidated(name = "ui.data.column.stock.modifyNum", number = true, min = -999999, max = 999999)
    @TableField("MODIFY_NUM")
    private BigDecimal modifyNum;

    /**
     * 不良数量
     */
    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.badNum")
    @ImportValidated(name = "ui.data.column.stock.badNum", number = true, min = 0, max = 999999)
    @TableField("BAD_NUM")
    private BigDecimal badNum;

    /**
     * 库存量(卷)
     */
    @ApiModelProperty(value = "库存量(卷)", position = 70)
    @Excel(name = "ui.data.column.stock.rollStockNum", scale = 1)
    @ImportValidated(name = "ui.data.column.stock.rollStockNum", number = true, min = 0, max = 999999)
    @TableField("ROLL_STOCK_NUM")
    private BigDecimal rollStockNum;

    /**
     * 修正数量(卷)
     */
    @ApiModelProperty(value = "修正数量(卷)", position = 80)
    @Excel(name = "ui.data.column.stock.rollModifyNum")
    @ImportValidated(name = "ui.data.column.stock.rollModifyNum", number = true, min = -999999, max = 999999)
    @TableField("ROLL_MODIFY_NUM")
    private BigDecimal rollModifyNum;

    /**
     * 不良数量(卷)
     */
    @ApiModelProperty(value = "不良数量(卷)", position = 90)
    @Excel(name = "ui.data.column.stock.rollBadNum")
    @ImportValidated(name = "ui.data.column.stock.rollBadNum", number = true, min = 0, max = 999999)
    @TableField("ROLL_BAD_NUM")
    private BigDecimal rollBadNum;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(name = "ui.data.column.stock.remark", maxLength = 300)
    private String remark;

    /**
     * 卷曲长度。此胎面一卷的最大长度，单位：米。
     */
    @ApiModelProperty(value = "卷曲长度。此胎面一卷的最大长度，单位：米。")
    @TableField(exist = false)
    private BigDecimal curlLength;
}
