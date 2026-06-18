package com.zlt.aps.dj.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 垫胶机台维修计划对象 t_dj_machine_maintenance
 */
@ApiModel(value = "垫胶机台维修计划", description = "垫胶机台维修计划")
@Data
@TableName(value = "T_DJ_MACHINE_MAINTENANCE")
public class DjMachineMaintenance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.dj.machineMaintenance.factoryCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.dj.machineMaintenance.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.dj.machineMaintenance.stopStartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "停机开始时间", name = "stopStartTime")
    @TableField(value = "STOP_START_TIME")
    private Date stopStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "ui.data.column.dj.machineMaintenance.stopEndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "停机结束时间", name = "stopEndTime")
    @TableField(value = "STOP_END_TIME")
    private Date stopEndTime;

    /**
     * 停机班次，对应 ClassNumThreePlanEnums.classIndex："01"=夜班、"02"=早班、"03"=中班
     */
    @Excel(name = "ui.data.column.dj.machineMaintenance.stopShift", dictType = "CLASS_NUM")
    @ApiModelProperty(value = "停机班次，对应 ClassNumThreePlanEnums.classIndex：01=夜班、02=早班、03=中班", name = "stopShift")
    @TableField(value = "STOP_SHIFT")
    private String stopShift;

    /**
     * 机台名称
     */
    @Excel(name = "ui.data.column.dj.machineMaintenance.machineName")
    @ApiModelProperty(value = "机台名称", name = "machineName")
    @TableField(exist = false)
    private String machineName;
}
