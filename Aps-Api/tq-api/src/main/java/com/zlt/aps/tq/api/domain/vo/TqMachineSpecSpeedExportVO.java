package com.zlt.aps.tq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TqMachineSpecSpeedExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    @Excel(name = "ui.tq.machineSpecSpeed.column.beadCode")
    private String beadCode;

    @Excel(name = "ui.tq.machineSpecSpeed.column.standardSpeed")
    private BigDecimal standardSpeed;

    @Excel(name = "ui.tq.machineSpecSpeed.column.quota")
    private Integer quota;

    @Excel(name = "ui.tq.machineSpecSpeed.column.quotaMes")
    private Integer quotaMes;

    @Excel(name = "ui.common.column.remark")
    private String remark;

    @Excel(name = "ui.common.column.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
