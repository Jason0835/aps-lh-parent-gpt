package com.zlt.aps.monthplan.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmDevicePlanShut.java
 * 描    述：0106基础数据_设备计划停机对象 t_mdm_device_plan_shut
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-04
 */
@ApiModel(value = "0106基础数据_设备计划停机对象", description = "0106基础数据_设备计划停机对象 ")
@Data
@TableName(value = "T_MDM_DEVICE_PLAN_SHUT")
public class MdmDevicePlanShut extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号，字典：biz_factory_name
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 工序，字典：work_calendar_proc；01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06--内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.procCode", dictType = "work_calendar_proc")
    @ApiModelProperty(value = "工序，字典：work_calendar_proc；01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06--内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼", name = "procCode")
    @TableField(value = "PROC_CODE")
    private String procCode;

    /**
     * 机台类型，字典：machine_type；硫化、成型、压出、裁断、压延、密炼；
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.machineType", dictType = "machine_type")
    @ApiModelProperty(value = "机台类型，字典：machine_type；硫化、成型、压出、裁断、压延、密炼；", name = "machineType")
    @TableField(value = "MACHINE_TYPE")
    private String machineType;

    /**
     * 机台编号
     */
    @ImportExcelValidated(required = true, maxLength = 50)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    /**
     * 停机类型，字典：machine_stop_type；01-精度校验、02-润滑、03-巡检点检、04-预见性维护、05-预防性维护、06-计划性维修
     */
    @ImportExcelValidated(required = true)
    @Excel(name = "ui.data.column.mdmDevicePlanShut.machineStopType", dictType = "machine_stop_type")
    @ApiModelProperty(value = "停机类型，字典：machine_stop_type；01-精度校验、02-润滑、03-巡检点检、04-预见性维护、05-预防性维护、06-计划性维修", name = "machineStopType")
    @TableField(value = "MACHINE_STOP_TYPE")
    private String machineStopType;

    /**
     * 开始日期:yyyy-MM-DD HH
     */
    @ImportExcelValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.mdmDevicePlanShut.beginDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "开始日期:yyyy-MM-dd HH:mm:ss", name = "beginDate")
    @TableField(value = "BEGIN_DATE")
    private Date beginDate;

    /**
     * 结束日期:yyyy-MM-DD HH
     */
    @ImportExcelValidated(required = true, date = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.mdmDevicePlanShut.endDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "结束日期:yyyy-MM-dd HH:mm:ss", name = "endDate")
    @TableField(value = "END_DATE")
    private Date endDate;


}
