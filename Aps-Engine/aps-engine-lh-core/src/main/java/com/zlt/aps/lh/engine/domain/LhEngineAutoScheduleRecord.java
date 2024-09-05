package com.zlt.aps.lh.engine.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 硫化工序自动排程抓取记录表
 */
@Data
public class LhEngineAutoScheduleRecord  extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 硫化排程批次号 */
    @Excel(name = "ui.data.column.record.lhBatchNo")
    @ApiModelProperty(value = "硫化自动排程批次号")
    private String lhBatchNo;

    /** 成型自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号 */
    @Excel(name = "ui.data.column.record.cxBatchNo")
    @ApiModelProperty(value = "成型自动排程批次号信息，每重新生成一次排程结果，批次号就递增。规则：工序+年月日+3位定长自增序号")
    private String cxBatchNo;

    /** 抓取状态：0-成功；1-失败 */
    @Excel(name = "ui.data.column.record.status")
    @ApiModelProperty(value = "状态：0-成功；1-失败")
    private String status;

    /** 排程时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.record.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程时间")
    private Date scheduleDate;

    /** 删除标识（0未删除；1已删除） */
    @ApiModelProperty(value = "排程时间")
    private String delFlag;

    /**
     *  自动排程日期条件
     */
    private String autoScheduleDate;
}
