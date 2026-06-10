package com.zlt.aps.tm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@ApiModel(value = "胎面机台维修计划", description = "胎面机台维修计划")
@Data
@TableName(value = "T_TM_MACHINE_MAINTENANCE")
public class TmMachineMaintenance extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.tm.MachineMaintenance.factoryCode")
    @ApiModelProperty(value = "工厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @Excel(name = "ui.data.column.tm.MachineMaintenance.machineCode")
    @ApiModelProperty(value = "机台编码", name = "machineCode")
    @TableField(value = "MACHINE_CODE")
    private String machineCode;

    @Excel(name = "ui.data.column.tm.MachineMaintenance.stopStartTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "停机开始时间", name = "stopStartTime")
    @TableField(value = "STOP_START_TIME")
    private Date stopStartTime;

    @Excel(name = "ui.data.column.tm.MachineMaintenance.stopEndTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "停机结束时间", name = "stopEndTime")
    @TableField(value = "STOP_END_TIME")
    private Date stopEndTime;
}
