package com.zlt.aps.tq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 胎圈口型板导出VO
 */
@Data
public class TqMouthPlateExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.data.column.mouthPlateCode")
    private String mouthPlateCode;

    @Excel(name = "ui.specifyMachine.column.machineName")
    private String machineName;

    @Excel(name = "ui.data.column.mouthPlateStatus", dictType = "STATUS")
    private String status;

    @Excel(name = "ui.common.column.remark")
    private String remark;

    @Excel(name = "ui.common.column.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
