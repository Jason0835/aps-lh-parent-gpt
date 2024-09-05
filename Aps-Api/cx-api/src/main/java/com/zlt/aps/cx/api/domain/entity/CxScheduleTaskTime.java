package com.zlt.aps.cx.api.domain.entity;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 成型排程任务时间对象 t_cx_schedule_task_time
 * 
 * @author Joran.zhang
 * @date 2022-05-17
 */
@ApiModel(value = "成型排程任务时间对象", description = "成型排程任务时间对象 ")
@Data
public class CxScheduleTaskTime extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_CX_SCHEDULE_TASK_TIME */
    @ApiModelProperty(value = "${comment}")
    private Long id;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 成型排程工单号 */
    @ApiModelProperty(value = "成型排程工单号")
    private String cxOrderNo;

    /** 机台编号 */
    @ApiModelProperty(value = "机台编号")
    private String machineCode;

    /** 生产顺序 */
    @ApiModelProperty(value = "生产顺序")
    private Integer productOrder;

    /** 预计开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date estimateStartTime;

    /** 预计结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "预计结束时间")
    private Date estimateEndTime;

    /** 数据来源：0&gt;自动排程；1&gt;排程导入 */
    @ApiModelProperty(value = "数据来源：0&gt;自动排程；1&gt;排程导入")
    private String dataSource;





}
