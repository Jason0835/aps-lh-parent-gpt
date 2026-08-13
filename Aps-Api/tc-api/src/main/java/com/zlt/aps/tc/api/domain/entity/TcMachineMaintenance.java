package com.zlt.aps.tc.api.domain.entity;

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

@ApiModel(value = "胎侧机台维修计划", description = "胎侧机台维修计划")
@Data
@TableName(value = "T_TC_MACHINE_MAINTENANCE")
public class TcMachineMaintenance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tc.machineMaintenance.factoryCode", dictType = "biz_factory_name")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tc.machineMaintenance.machineCode")
    @ImportExcelValidated(required = true, isCode = true, maxLength = 50)
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.tc.machineMaintenance.stopStartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportExcelValidated(required = true, date = true)
    @ApiModelProperty(value = "停机开始时间", name = "stopStartTime")
    @TableField(value = "STOP_START_TIME")
    private Date stopStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "ui.data.column.tc.machineMaintenance.stopEndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ImportExcelValidated(required = true, date = true)
    @ApiModelProperty(value = "停机结束时间", name = "stopEndTime")
    @TableField(value = "STOP_END_TIME")
    private Date stopEndTime;

    @Excel(name = "ui.data.column.tc.machineMaintenance.stopShift", dictType = "class_num_three_plan")
    @ImportExcelValidated(maxLength = 20)
    @ApiModelProperty(value = "停机班次", name = "stopShift")
    @TableField(value = "STOP_SHIFT")
    private String stopShift;

    @Excel(name = "ui.common.column.remark")
    @ImportExcelValidated(maxLength = 500)
    @ApiModelProperty("备注")
    @TableField("REMARK")
    private String remark;
}
