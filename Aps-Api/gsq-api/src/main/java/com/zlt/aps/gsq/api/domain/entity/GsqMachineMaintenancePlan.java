package com.zlt.aps.gsq.api.domain.entity;

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

/**
 * 钢丝圈机台维修计划对象 t_gsq_machine_maintenance_plan
 *
 * @author zlt
 * @date 2026-07-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("T_GSQ_MACHINE_MAINTENANCE_PLAN")
@ApiModel(value = "钢丝圈机台维修计划对象", description = "钢丝圈机台维修计划对象")
public class GsqMachineMaintenancePlan extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 机台编码 */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.machineName")
    @ApiModelProperty(value = "机台编码", position = 20)
    @TableField("MACHINE_CODE")
    @ImportValidated(required = true, maxLength = 50)
    private String machineCode;

    /** 机台名称（反显字段，非数据库字段，仅供列表/导出显示，不出现在导入模板） */
    @ApiModelProperty(value = "机台名称", position = 25)
    @TableField(exist = false)
    private String machineName;

    /** 停机日期 */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.downtimeDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "停机日期", position = 30)
    @TableField("DOWNTIME_DATE")
    @ImportValidated(required = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date downtimeDate;

    /** 停机班次 */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.downtimeShift", dictType = "class_num_three_plan")
    @ApiModelProperty(value = "停机班次", position = 40)
    @TableField("DOWNTIME_SHIFT")
    @ImportValidated(required = true, maxLength = 20)
    private String downtimeShift;

    /** 停机时间(H) */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.downtimeHours")
    @ApiModelProperty(value = "停机时间(H)", position = 50)
    @TableField("DOWNTIME_HOURS")
    @ImportValidated(number = true, min = 0, max = 999999)
    private BigDecimal downtimeHours;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注", position = 500)
    @TableField("REMARK")
    @ImportValidated(maxLength = 900)
    private String remark;

    /** 停机日期范围-开始（查询用，非数据库字段） */
    @TableField(exist = false)
    private Date downtimeDateBegin;

    /** 停机日期范围-结束（查询用，非数据库字段） */
    @TableField(exist = false)
    private Date downtimeDateEnd;
}
