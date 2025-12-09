package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryProductionVersion.java
 * 描    述：分厂月度计划模具状态日志记录对象 T_MP_MOULD_USE_STATUS_LOG
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20251208
 */

@Data
@TableName(value = "T_MP_MOULD_USE_STATUS_LOG")
@ApiModel(value = "分厂月度计划模具状态日志记录对象", description = "分厂月度计划模具状态日志记录对象")
public class FactoryMouldUsedLog extends BaseEntity {
    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mouldStatusLog.year")
    @ImportExcelValidated(required = true, digits = true, min = 1000, max = 9999)
    @ApiModelProperty(value = "年份")
    @TableField(value = "YEAR")
    private Long year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mouldStatusLog.month")
    @ImportExcelValidated(required = true, digits = true, min = 1, max = 12)
    @ApiModelProperty(value = "月份")
    @TableField(value = "MONTH")
    private Long month;

    /**
     * 可用分厂编号
     */
    @Excel(name = "ui.data.column.mouldStatusLog.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 20)
    @ApiModelProperty(value = "可用分厂编号")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.mouldStatusLog.mouldCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 40)
    @ApiModelProperty(value = "模具号")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.productionMonthPlanInit.planType", dictType = "biz_plan_type")
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 模具状态
     */
    @ImportExcelValidated(required = true, digits = true)
    @Excel(name = "ui.data.column.mouldStatusLog.mouldStatus", dictType = "biz_available_status")
    @ApiModelProperty(value = "模具状态")
    @TableField(value = "MOULD_STATUS")
    private Long mouldStatus;

    /**
     * 归属分厂
     */
    @Excel(name = "ui.data.column.mouldStatusLog.ownerFactoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "归属分厂")
    @TableField(value = "OWNER_FACTORY_CODE")
    private String ownerFactoryCode;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.mouldStatusLog.specifications", width = 50, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "规格")
    @TableField(value = "SPECIFICATIONS")
    private String specifications;

    /**
     * 花纹
     */
    @Excel(name = "ui.data.column.mouldStatusLog.pattern", width = 40, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "花纹")
    @TableField(value = "PATTERN")
    private String pattern;

    /**
     * 模具类型
     */
    @Excel(name = "ui.data.column.mouldStatusLog.mouldType", dictType = "biz_mould_Type")
    @ApiModelProperty(value = "模具类型")
    @TableField(value = "MOULD_TYPE")
    private String mouldType;

    @Excel(name = "ui.data.column.mouldStatusLog.remark", width = 40, align = Excel.Align.LEFT)
    @ImportExcelValidated(maxLength = 1000)
    @ApiModelProperty(value = "备注")
    @TableField(value = "REMARK")
    private String remark;
}
