package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TireCapacityConfiguration.java
 * 描    述：轮胎类型产能配置(特殊情况下配置)对象 t_mdm_tire_capacity
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-06-04
 */

@Data
@TableName(value = "T_MDM_TIRE_CAPACITY")
@ApiModel(value = "轮胎类型产能配置(特殊情况下配置)对象", description = "轮胎类型产能配置(特殊情况下配置)对象 ")
public class TireCapacityConfiguration extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分厂编号
     */
    @Excel(name = "ui.data.column.tireCapacity.factoryCode")
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.tireCapacity.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.tireCapacity.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.tireCapacity.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 轮胎类型 取数据字典 biz_tire_type的编码
     */
    @Excel(name = "ui.data.column.tireCapacity.tireType")
    @ApiModelProperty(value = "轮胎类型 取数据字典 biz_tire_type的编码", name = "tireType")
    @TableField(value = "TIRE_TYPE")
    private String tireType;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.tireCapacity.proSize", readConverterExp = "保=留2位小数")
    @ApiModelProperty(value = "寸口", name = "proSize")
    @TableField(value = "PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 月总产能
     */
    @Excel(name = "ui.data.column.tireCapacity.monthCapacity")
    @ApiModelProperty(value = "月总产能", name = "monthCapacity")
    @TableField(value = "MONTH_CAPACITY")
    private Integer monthCapacity;

    /**
     * 轮胎类型 + 寸口
     *
     * @return
     */
    public String getGroupKey() {
        String groupKey = "%s|*|%s";
        return String.format(groupKey, getTireType(), getProSize());
    }

}