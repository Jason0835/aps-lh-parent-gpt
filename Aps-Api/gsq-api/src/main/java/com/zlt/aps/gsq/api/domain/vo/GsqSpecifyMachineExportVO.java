package com.zlt.aps.gsq.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 钢丝圈定点机台导出VO
 * 用于导出时反显生产线名称等非数据库字段
 *
 * @author zlt
 * @date 2026-07-08
 */
@Data
public class GsqSpecifyMachineExportVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsq.specifyMachine.steelRingCode")
    private String steelRingCode;

    /** 生产线（机台编码） */
    @Excel(name = "ui.data.column.gsq.specifyMachine.machineCode")
    private String machineCode;

    /** 生产线名称（反显） */
    @Excel(name = "ui.data.column.gsq.specifyMachine.machineName")
    private String machineName;

    /** 线路类型 */
    @Excel(name = "ui.data.column.gsq.specifyMachine.lineType", dictType = "LINE_TYPE")
    private String lineType;

    /** 作业类型 */
    @Excel(name = "ui.data.column.gsq.specifyMachine.jobType", dictType = "JOB_TYPE")
    private String jobType;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    private String remark;

    /** 更新时间 */
    @Excel(name = "ui.data.column.gsq.specifyMachine.updateDate", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
