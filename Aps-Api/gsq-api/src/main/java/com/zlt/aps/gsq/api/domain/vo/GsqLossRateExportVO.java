package com.zlt.aps.gsq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 钢丝圈损耗率管理导出VO
 * 用于导出时反显机台名称等非数据库字段
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
public class GsqLossRateExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 钢丝圈编码 */
    @Excel(name = "ui.data.column.gsq.lossRate.steelRingCode")
    private String steelRingCode;

    /** 机台名称（反显） */
    @Excel(name = "ui.data.column.gsq.lossRate.machineName")
    private String machineName;

    /** 损耗率(百分比) */
    @Excel(name = "ui.data.column.gsq.lossRate.lossRate")
    private BigDecimal lossRate;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    private String remark;

    /** 更新时间 */
    @Excel(name = "ui.data.column.gsq.lossRate.updateDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
