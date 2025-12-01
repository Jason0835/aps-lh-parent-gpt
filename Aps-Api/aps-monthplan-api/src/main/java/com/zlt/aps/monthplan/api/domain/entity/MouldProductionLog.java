package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MouldProductionLog.java
 * 描    述：分厂月度排产流程日志对象 t_mp_mould_production_log
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */

@Data
@TableName(value = "T_MP_MOULD_PRODUCTION_LOG")
@ApiModel(value = "分厂月度排产流程日志对象", description = "分厂月度排产流程日志对象 ")
public class MouldProductionLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 操作批次号
     */
    @Excel(name = "ui.data.column.mouldProductionLog.workNo")
    @ApiModelProperty(value = "操作批次号", name = "workNo")
    @TableField(value = "WORK_NO")
    private String workNo;
    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.mouldProductionLog.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mouldProductionLog.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mouldProductionLog.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.mouldProductionLog.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.mouldProductionLog.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 需求计划
     */
    @Excel(name = "ui.data.column.mouldProductionLog.monthPlanId")
    @ApiModelProperty(value = "需求计划", name = "monthPlanId")
    @TableField(value = "MONTH_PLAN_ID")
    private Long monthPlanId;

    /**
     * 物料编号
     */
    @ApiModelProperty(value = "物料编号", name = "productCode")
    @TableField(exist = false)
    private String productCode;
    /**
     * 日志类型 1 初始化 2 排产顺序分组 3 单计划排产 4 单计划交期排产 5 单计划通用排产 6 同模具交期分组排产 7 同规格分组排产 8 同模具无交期分组排产
     */
    @Excel(name = "ui.data.column.mouldProductionLog.logType")
    @ApiModelProperty(value = "日志类型 1 初始化 2 排产顺序分组 3 单计划排产 4 单计划交期排产 5 单计划通用排产 6 同模具交期分组排产 7 同规格分组排产 8 同模具无交期分组排产", name = "logType")
    @TableField(value = "LOG_TYPE")
    private Integer logType;

    /**
     * 日志内容
     */
    @Excel(name = "ui.data.column.mouldProductionLog.logContent")
    @ApiModelProperty(value = "日志内容", name = "logContent")
    @TableField(value = "LOG_CONTENT")
    private String logContent;

}