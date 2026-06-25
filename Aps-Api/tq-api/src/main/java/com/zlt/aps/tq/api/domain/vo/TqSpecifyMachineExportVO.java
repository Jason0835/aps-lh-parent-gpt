package com.zlt.aps.tq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈定点机台导出VO
 */
@Data
public class TqSpecifyMachineExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.specifyMachine.column.beadCode")
    private String beadCode;

    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    @Excel(name = "ui.specifyMachine.column.lineType", dictType = "LINE_TYPE")
    private String lineType;

    @Excel(name = "ui.specifyMachine.column.jobType", dictType = "JOB_TYPE")
    private String jobType;

    @Excel(name = "ui.common.column.remark")
    private String remark;

    @Excel(name = "ui.common.column.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
