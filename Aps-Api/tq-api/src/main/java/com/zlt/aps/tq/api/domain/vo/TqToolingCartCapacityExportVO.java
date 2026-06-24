package com.zlt.aps.tq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class TqToolingCartCapacityExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "ui.tq.toolingCartCapacity.column.beadCode")
    private String beadCode;

    @Excel(name = "ui.tq.toolingCartCapacity.column.cartCapacity")
    private Integer cartCapacity;

    @Excel(name = "ui.common.column.remark")
    private String remark;

    @Excel(name = "ui.tq.toolingCartCapacity.column.updateTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
