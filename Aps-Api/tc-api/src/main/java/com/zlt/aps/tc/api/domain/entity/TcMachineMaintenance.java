package com.zlt.aps.tc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：TcMachineMaintenance.java
 * 描    述：胎侧机台维修计划对象 t_tc_machine_maintenance
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-09-15
 */
@ApiModel(value = "胎侧机台维修计划对象", description = "胎侧机台维修计划对象 ")
@Data
@TableName(value = "T_TC_MACHINE_MAINTENANCE")
public class TcMachineMaintenance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 停机日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.machineMaintenance.stopDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "停机日期", name = "stopDate")
    @TableField(value = "STOP_DATE")
    private Date stopDate;

    /**
     * 停机开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    @Excel(name = "ui.data.column.machineMaintenance.stopStartTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "停机开始时间", name = "stopStartTime")
    @TableField(value = "STOP_START_TIME")
    private Date stopStartTime;

    /**
     * 停机结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    @Excel(name = "ui.data.column.machineMaintenance.stopEndTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "停机结束时间", name = "stopEndTime")
    @TableField(value = "STOP_END_TIME")
    private Date stopEndTime;

    /**
     * 机台id（对应T_TC_MACHINE_INFO表id）
     */
    @ApiModelProperty(value = "机台id", name = "machineId")
    @TableField(value = "MACHINE_ID")
    private Long machineId;

    /**
     * 停机时间(单位：H)
     */
    @Excel(name = "ui.data.column.machineMaintenance.stopTime", cellType = Excel.ColumnType.NUMERIC)
    @ApiModelProperty(value = "停机时间(单位：H)", name = "stopTime")
    @TableField(value = "STOP_TIME")
    private BigDecimal stopTime;

    /**
     * 停机班次，字典：CLASS_NUM
     * 根据停机时间解析，如果是19-7，则停机班次=2
     * 如果是7-19，则停机班次=3
     */
    @Excel(name = "ui.data.column.machineMaintenance.stopShift", dictType = "CLASS_NUM")
    @ApiModelProperty(value = "停机班次，字典：CLASS_NUM", name = "stopShift")
    @TableField(value = "STOP_SHIFT")
    private String stopShift;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.machineMaintenance.machineName")
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(exist = false)
    private String machineName;

    /**
     * 机台定额
     */
    @ApiModelProperty(value = "机台定额", name = "quota")
    @TableField(exist = false)
    private BigDecimal quota;
}
