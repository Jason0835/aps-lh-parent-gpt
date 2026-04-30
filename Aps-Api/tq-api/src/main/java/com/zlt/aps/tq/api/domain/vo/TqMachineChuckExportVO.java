package com.zlt.aps.tq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎圈机台寸口对应导出VO
 */
@Data
public class TqMachineChuckExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.machineChuck.column.machineName")
    private String machineName;

    @Excel(name = "ui.tq.machineChuck.column.chuckCode")
    private String chuckCode;

    @Excel(name = "ui.tq.machineChuck.column.chuckName")
    private String chuckName;

    @Excel(name = "ui.tq.machineChuck.column.inchSize")
    private BigDecimal inchSize;

    @Excel(name = "ui.common.column.remark")
    private String remark;

    @Excel(name = "ui.tq.machineChuck.column.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
