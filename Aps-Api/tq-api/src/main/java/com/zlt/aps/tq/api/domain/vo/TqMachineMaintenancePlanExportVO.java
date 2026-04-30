package com.zlt.aps.tq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TqMachineMaintenancePlanExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.machineName")
    private String machineName;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.downtimeDate", width = 30, dateFormat = "yyyy-MM-dd")
    private Date downtimeDate;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.downtimeShift", dictType = "biz_class_type")
    private String downtimeShift;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.downtimeHours")
    private BigDecimal downtimeHours;

    @Excel(name = "ui.common.column.remark")
    private String remark;

    @Excel(name = "ui.tq.machineMaintenancePlan.column.updateDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
