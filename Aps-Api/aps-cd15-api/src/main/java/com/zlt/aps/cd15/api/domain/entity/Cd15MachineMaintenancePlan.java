package com.zlt.aps.cd15.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 斜裁机台检修计划。
 */
@Data
@ApiModel(value = "斜裁机台检修计划", description = "斜裁机台检修计划")
@TableName("t_cd15_machine_maintenance_plan")
public class Cd15MachineMaintenancePlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */
    @ApiModelProperty("工厂编码")
    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 机台编码 */
    @ApiModelProperty("机台编码")
    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.machineCode")
    private String machineCode;

    /** 停机日期，按停机开始时间所属日期生成 */
    @ApiModelProperty("停机日期")
    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("DOWNTIME_DATE")
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.downtimeDate")
    private Date downtimeDate;

    /** 停机开始时间 */
    @ApiModelProperty("停机开始时间")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("DOWNTIME_START_TIME")
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.downtimeStartTime")
    private Date downtimeStartTime;

    /** 停机结束日期，前端跨日辅助字段，不落库 */
    @ApiModelProperty("停机结束日期")
    @TableField(exist = false)
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.downtimeEndDate")
    private Date downtimeEndDate;

    /** 停机结束时间 */
    @ApiModelProperty("停机结束时间")
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("DOWNTIME_END_TIME")
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.downtimeEndTime")
    private Date downtimeEndTime;

    /** 停机时长(小时) */
    @ApiModelProperty("停机时长(小时)")
    @TableField("DOWNTIME_HOURS")
    @Excel(name = "ui.data.column.cd15MachineMaintenancePlan.downtimeHours")
    private BigDecimal downtimeHours;
}