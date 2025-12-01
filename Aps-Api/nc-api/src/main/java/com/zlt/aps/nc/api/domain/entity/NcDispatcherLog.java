package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.Map;

/**
 * 内衬调度员排程操作日志对象 t_nc_dispatcher_log
 * 
 * @author Gim
 * @date 2022-02-25
 */
@ApiModel(value = "内衬调度员排程操作日志对象", description = "内衬调度员排程操作日志对象 ")
@Data
@TableName("t_nc_dispatcher_log")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_T_NC_DISPATCHER_LOG",dbType = DbType.ORACLE)
public class NcDispatcherLog extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_NC_DISPATCHER_LOG */
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "排程记录id")
    private Long scheduleId;

    /** 操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE */
    @Excel(name = "ui.data.column.nc.dispatcherlog.operType")
    @ApiModelProperty(value = "操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.nc.dispatcherlog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 内衬代码 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.materialCode")
    @ApiModelProperty(value = "内衬代码")
    private String materialCode;

    /** 操作前机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.beforeMachineId")
    @ApiModelProperty(value = "操作前机台ID,多个逗号分割")
    private String beforeMachineId;

    /** 操作前中班计划量 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.beforeDayPlan")
    @ApiModelProperty(value = "操作前中班计划量")
    private Double beforeDayPlan;

    /** 操作前夜班计划量 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.beforeNightPlan")
    @ApiModelProperty(value = "操作前夜班计划量")
    private Double beforeNightPlan;

    /** 操作后机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.afterMachineId")
    @ApiModelProperty(value = "操作后机台ID,多个逗号分割")
    private String afterMachineId;

    /** 操作后中班计划量 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.afterDayPlan")
    @ApiModelProperty(value = "操作后中班计划量")
    private Double afterDayPlan;

    /** 操作后夜班计划量 */
    @Excel(name = "ui.data.column.nc.dispatcherlog.afterNightPlan")
    @ApiModelProperty(value = "操作后夜班计划量")
    private Double afterNightPlan;

    private transient String startTime;

    private transient String endTime;

    /**
     * 用于导出转换操作类型字典项
     */
    private Map<String,String> operationTypeDictMap;
}
