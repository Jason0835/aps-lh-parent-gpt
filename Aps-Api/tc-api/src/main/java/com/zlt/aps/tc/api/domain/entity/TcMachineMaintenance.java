package com.zlt.aps.tc.api.domain.entity;

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
import java.math.BigDecimal;

@ApiModel(value = "胎侧机台维修计划", description = "胎侧机台维修计划")
@Data
@TableName(value = "T_TC_MACHINE_MAINTENANCE")
public class TcMachineMaintenance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.machineMaintenance.factoryCode", dictType = "biz_factory_name")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.machineMaintenance.machineCode")
    @ImportValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.tc.machineMaintenance.stopStartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "停机开始时间", name = "stopStartTime")
    @TableField(value = "STOP_START_TIME")
    private Date stopStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.tc.machineMaintenance.stopEndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportValidated(required = true, date = true)
    @ApiModelProperty(value = "停机结束时间", name = "stopEndTime")
    @TableField(value = "STOP_END_TIME")
    private Date stopEndTime;

    @Excel(name = "ui.data.column.tc.machineMaintenance.stopShift", dictType = "class_num_three_plan")
    @ImportValidated(maxLength = 20)
    @ApiModelProperty(value = "停机班次", name = "stopShift")
    @TableField(value = "STOP_SHIFT")
    private String stopShift;

    /** 兼容按生产日维护的停机日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.tc.machineMaintenance.stopDate", width = 20, dateFormat = "yyyy-MM-dd")
    @ImportValidated(date = true)
    @ApiModelProperty(value = "停机日期", name = "stopDate")
    @TableField(value = "STOP_DATE")
    private Date stopDate;

    /** 兼容按班次维护的停机时长，单位小时 */
    @Excel(name = "ui.data.column.tc.machineMaintenance.stopHours")
    @ApiModelProperty(value = "停机时长（小时）", name = "stopHours")
    @TableField(value = "STOP_TIME_HOURS")
    private BigDecimal stopHours;

    @Excel(name = "ui.common.column.remark")
    @ImportValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
