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
 * 描    述：S2-0410.排产过程_排产日志对象 t_mp_mould_production_log
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
@TableName(value = "T_MP_MOULD_PRODUCTION_LOG")
@ApiModel(value = "工厂月度排产流程日志对象", description = "工厂月度排产流程日志对象")
public class MouldProductionLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编码
     */
    @Excel(name = "ui.data.column.mouldProductionLog.factoryCode")
    @ApiModelProperty(value = "工厂编码", name = "factoryCode")
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
     * 排产版本
     */
    @Excel(name = "ui.data.column.mouldProductionLog.productionVersion")
    @ApiModelProperty(value = "排产版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 计划类型 01 正常 02 订单预测 03 实单模拟
     */
    @Excel(name = "ui.data.column.cxCapacity.planType", dictType = "biz_plan_type")
    @ApiModelProperty(value = "计划类型", name = "planType")
    @TableField(value = "PLAN_TYPE")
    private String planType;

    /**
     * 操作批次号
     */
    @Excel(name = "ui.data.column.mouldProductionLog.workNo")
    @ApiModelProperty(value = "操作批次号", name = "workNo")
    @TableField(value = "WORK_NO")
    private String workNo;


    /**
     * 日志内容
     */
    @Excel(name = "ui.data.column.mouldProductionLog.logContent")
    @ApiModelProperty(value = "日志内容", name = "logContent")
    @TableField(value = "LOG_CONTENT")
    private String logContent;

}