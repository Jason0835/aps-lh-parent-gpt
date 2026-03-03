package com.zlt.aps.mdm.api.domain.entity;

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
 * 文件名称：MdmWorkCalendar.java
 * 描    述：工作日历对象 t_mdm_work_calendar
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
@ApiModel(value = "工作日历对象", description = "工作日历对象 ")
@Data
@TableName(value = "T_MDM_WORK_CALENDAR")
public class MdmWorkCalendar extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 工厂编号
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06--内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.procCode", dictType = "work_calendar_proc")
    @ApiModelProperty(value = "01-月计划,02-硫化,03-成型,04-胎面,05-胎侧,06--内衬,07-垫胶,08-胎圈,09-钢丝圈,10-斜裁,11-直裁,12-压延,15-零度,16-密炼", name = "procCode")
    @TableField(value = "PROC_CODE")
    private String procCode;

    /**
     * 年份
     */
    @ImportExcelValidated(required = true, min = 0, max = 9999)
    @Excel(name = "ui.data.column.mdmWorkCalendar.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @ImportExcelValidated(required = true, min = 1, max = 12)
    @Excel(name = "ui.data.column.mdmWorkCalendar.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 日期
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.day")
    @ApiModelProperty(value = "日期", name = "day")
    @TableField(value = "DAY")
    private Integer day;

    /**
     * 日期
     */
    @ApiModelProperty(value = "日期", name = "productionDate")
    @TableField(value = "PRODUCTION_DATE")
    private Date productionDate;

    /**
     * 一班开停产标志，0-停,1-开
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.oneShiftFlag", dictType = "sys_enable_disable")
    @ApiModelProperty(value = "一班开停产标志，0-停,1-开", name = "oneShiftFlag")
    @TableField(value = "ONE_SHIFT_FLAG")
    private String oneShiftFlag;

    /**
     * 二班开停产标志，0-停,1-开
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.twoShiftFlag", dictType = "sys_enable_disable")
    @ApiModelProperty(value = "二班开停产标志，0-停,1-开", name = "twoShiftFlag")
    @TableField(value = "TWO_SHIFT_FLAG")
    private String twoShiftFlag;

    /**
     * 三班开停产标志，0-停,1-开
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.threeShiftFlag", dictType = "sys_enable_disable")
    @ApiModelProperty(value = "三班开停产标志，0-停,1-开", name = "threeShiftFlag")
    @TableField(value = "THREE_SHIFT_FLAG")
    private String threeShiftFlag;

    /**
     * 日期开停产标志，0-停,1-开
     */
    @Excel(name = "ui.data.column.mdmWorkCalendar.dayFlag", dictType = "sys_enable_disable")
    @ApiModelProperty(value = "日期开停产标志，0-停,1-开", name = "dayFlag")
    @TableField(value = "DAY_FLAG")
    private String dayFlag;

    /**
     * 比例
     */
    @ImportExcelValidated(digits = true, min = 0, max = 100)
    @Excel(name = "ui.data.column.mdmWorkCalendar.rate")
    @ApiModelProperty(value = "比例", name = "rate")
    @TableField(value = "RATE")
    private Integer rate;

    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "日期", name = "calendarTime")
    @TableField(exist = false)
    private Date calendarTime;

    /**
     * 节假日名称
     */
    @ApiModelProperty(value = "节假日名称", name = "holidayNames")
    @TableField(exist = false)
    private String holidayNames;
}
