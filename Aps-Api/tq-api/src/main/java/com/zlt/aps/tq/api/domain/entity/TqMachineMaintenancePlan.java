package com.zlt.aps.tq.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_TQ_MACHINE_MAINTENANCE_PLAN")
@ApiModel(value = "胎圈机台维修计划对象", description = "胎圈机台维修计划对象")
public class TqMachineMaintenancePlan extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "分厂编码", position = 10)
    @TableField("FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "机台ID", position = 20)
    @TableField("MACHINE_ID")
    private Long machineId;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.machineName")
    @ApiModelProperty(value = "机台名称", position = 25)
    @TableField(exist = false)
    private String machineName;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.downtimeDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "停机日期", position = 30)
    @TableField("DOWNTIME_DATE")
    @ImportValidated(required = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date downtimeDate;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.downtimeShift", dictType = "biz_class_type")
    @ApiModelProperty(value = "停机班次", position = 40)
    @TableField("DOWNTIME_SHIFT")
    @ImportValidated(required = true, maxLength = 20)
    private String downtimeShift;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.downtimeHours")
    @ApiModelProperty(value = "停机时间(H)", position = 50)
    @TableField("DOWNTIME_HOURS")
    private BigDecimal downtimeHours;

    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;

    @TableField(exist = false)
    private Date downtimeDateBegin;

    @TableField(exist = false)
    private Date downtimeDateEnd;
}
