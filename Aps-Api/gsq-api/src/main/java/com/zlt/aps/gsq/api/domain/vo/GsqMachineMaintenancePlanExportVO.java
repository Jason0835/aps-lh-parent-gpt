package com.zlt.aps.gsq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢丝圈机台维修计划导出VO
 * 用于导出时反显机台名称等非数据库字段
 *
 * @author zlt
 * @date 2026-07-01
 */
@Data
public class GsqMachineMaintenancePlanExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 机台名称（反显） */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.machineName")
    private String machineName;

    /** 停机日期 */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.downtimeDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date downtimeDate;

    /** 停机班次 */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.downtimeShift", dictType = "class_num_three_plan")
    private String downtimeShift;

    /** 停机时间(H) */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.downtimeHours")
    private BigDecimal downtimeHours;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    private String remark;

    /** 更新时间 */
    @Excel(name = "ui.data.column.gsq.machineMaintenancePlan.updateDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
