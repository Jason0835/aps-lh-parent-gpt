package com.zlt.aps.cd90.api.domain.entity;

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
 * 直裁机台检修计划。
 */
@Data
@ApiModel(value = "直裁机台检修计划", description = "直裁机台检修计划")
@TableName("t_cd90_machine_maintenance_plan")
public class Cd90MachineMaintenancePlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工厂编码 */

    @ImportExcelValidated(required = true, maxLength = 50)
    @TableField("FACTORY_CODE")
    @Excel(name = "ui.data.column.machineMaintenancePlan.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

    /** 机台编码 */

    @ImportExcelValidated(required = true, maxLength = 30)
    @TableField("MACHINE_CODE")
    @Excel(name = "ui.data.column.machineMaintenancePlan.machineCode")
    private String machineCode;

    /** 停机日期 */

    @ImportExcelValidated(required = true, maxLength = 10)
    @TableField("DOWNTIME_DATE")
    @Excel(name = "ui.data.column.machineMaintenancePlan.downtimeDate")
    private Date downtimeDate;

    /** 停机开始时间 */

    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("DOWNTIME_START_TIME")
    @Excel(name = "ui.data.column.machineMaintenancePlan.downtimeStartTime")
    private Date downtimeStartTime;

    /** 停机结束时间 */
    @ImportExcelValidated(required = true, maxLength = 20)
    @TableField("DOWNTIME_END_TIME")
    @Excel(name = "ui.data.column.machineMaintenancePlan.downtimeEndTime")
    private Date downtimeEndTime;

    /** 停机时长(小时) */
    @TableField("DOWNTIME_HOURS")
    @Excel(name = "ui.data.column.machineMaintenancePlan.downtimeHours")
    private BigDecimal downtimeHours;
}