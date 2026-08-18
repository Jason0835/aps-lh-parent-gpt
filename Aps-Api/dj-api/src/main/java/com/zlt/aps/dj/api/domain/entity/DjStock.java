package com.zlt.aps.dj.api.domain.entity;

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
 * 垫胶库存信息对象
 *
 * @author zlt
 * @date 2026-05-31
 */
@Data
@EqualsAndHashCode(callSuper=false)
@TableName(value = "T_DJ_STOCK")
@ApiModel(value = "垫胶库存信息对象", description = "垫胶库存信息对象")
public class DjStock extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.stock.stockDate", dateFormat = "yyyy-MM-dd")
    @ImportValidated(name = "ui.data.column.stock.stockDate", required = true, date = true)
    @ApiModelProperty(value = "库存日期", position = 20)
    @TableField(value = "STOCK_DATE")
    private Date stockDate;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd", position = 21)
    @TableField(exist = false)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd", position = 22)
    @TableField(exist = false)
    private String endTime;

    /**
     * 垫胶编号
     */
    @ApiModelProperty(value = "垫胶编号", position = 30)
    @Excel(name = "ui.data.column.dj.scheduleResult.paddingCode")
    @ImportValidated(name = "ui.data.column.dj.scheduleResult.paddingCode", required = true, maxLength = 50, isCode = true)
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /**
     * 垫胶名称
     */
    @ApiModelProperty(value = "垫胶名称", position = 30)
    @Excel(name = "ui.data.column.dj.scheduleResult.paddingName")
    @ImportValidated(name = "ui.data.column.dj.scheduleResult.paddingName", required = true, maxLength = 50, isCode = true)
    @TableField(value = "MATERIAL_NAME")
    private String materialName;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.stockNum")
    @ImportValidated(name = "ui.data.column.stock.stockNum", number = true, min = 0, max = 999999)
    @TableField(value = "STOCK_NUM")
    private BigDecimal stockNum;

    /**
     * 修正数量
     */
    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modifyNum")
    @ImportValidated(name = "ui.data.column.stock.modifyNum", number = true, min = -999999, max = 999999)
    @TableField(value = "MODIFY_NUM")
    private BigDecimal modifyNum;

    /**
     * 不良数量
     */
    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.badNum")
    @ImportValidated(name = "ui.data.column.stock.badNum", number = true, min = 0, max = 999999)
    @TableField(value = "BAD_NUM")
    private BigDecimal badNum;

    /**
     * 数据来源
     */
    @ApiModelProperty(value = "数据来源", position = 70)
    @TableField(value = "DATA_SOURCE")
    private String DataSource;
    
    /**
     * 卷曲长度。此胎面一卷的最大长度，单位：米。
     */
    @ApiModelProperty(value = "卷曲长度。此胎面一卷的最大长度，单位：米。")
    @TableField(exist = false)
    private BigDecimal curlLength;

    @Excel(name = "ui.data.column.info.remark")
    @ImportValidated(name = "ui.data.column.info.remark", maxLength = 100)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;

}
