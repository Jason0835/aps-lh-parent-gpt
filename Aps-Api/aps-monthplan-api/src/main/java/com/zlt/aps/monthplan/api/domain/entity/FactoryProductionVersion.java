package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryProductionVersion.java
 * 描    述：分厂月度计划排程版本对象 t_mp_proc_version
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-19
 */

@Data
@TableName(value = "T_MP_PROC_VERSION")
@ApiModel(value = "分厂月度计划排程版本对象", description = "分厂月度计划排程版本对象 ")
public class FactoryProductionVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 品名编码
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.productTypeCode")
    @ApiModelProperty(value = "品名编码", name = "productTypeCode")
    @TableField(value = "PRODUCT_TYPE_CODE")
    private String productTypeCode;

    /**
     * 品名
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.productTypeName")
    @ApiModelProperty(value = "品名", name = "productTypeName")
    @TableField(value = "PRODUCT_TYPE_NAME")
    private String productTypeName;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 初始化版本
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.productionInitVersion")
    @ApiModelProperty(value = "初始化版本", name = "productionInitVersion")
    @TableField(value = "PRODUCTION_INIT_VERSION")
    private String productionInitVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 月份排产起始日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产起始日", name = "productionStartDate")
    @TableField(value = "PRODUCTION_START_DATE")
    private Date productionStartDate;

    /**
     * 月份排产最大结束日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "月份排产最大结束日", name = "productionEndDate")
    @TableField(value = "PRODUCTION_END_DATE")
    private Date productionEndDate;

    /**
     * 0 不是自然月 1 是自然月
     */
    @ApiModelProperty(value = "0 不是自然月 1 是自然月", name = "isNaturalMonth")
    @TableField(value = "IS_NATURAL_MONTH")
    private Integer isNaturalMonth;

    /**
     * 0 不是定稿 1 是定稿
     */
    @Excel(name = "ui.data.column.factoryProductionVersion.isFinal")
    @ApiModelProperty(value = "0 不是定稿 1 是定稿", name = "isFinal")
    @TableField(value = "IS_FINAL")
    private Integer isFinal;
}