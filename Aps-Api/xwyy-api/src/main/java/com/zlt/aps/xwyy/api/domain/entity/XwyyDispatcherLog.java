package com.zlt.aps.xwyy.api.domain.entity;

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
 * 纤维压延调度员排程操作日志对象 t_xwyy_dispatcher_log
 * 
 * @author Gim
 * @date 2022-02-25
 */
@ApiModel(value = "纤维压延调度员排程操作日志对象", description = "纤维压延调度员排程操作日志对象 ")
@Data
@TableName("t_xwyy_dispatcher_log")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_T_XWYY_DISPATCHER_LOG",dbType = DbType.ORACLE)
public class XwyyDispatcherLog extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_XWYY_DISPATCHER_LOG */
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "排程记录id")
    private Long scheduleId;

    /** 操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.operType")
    @ApiModelProperty(value = "操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 纤维压延代码 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.materialCode")
    @ApiModelProperty(value = "纤维压延代码")
    private String materialCode;

    /** 操作前机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.beforeMachineId")
    @ApiModelProperty(value = "操作前机台ID,多个逗号分割")
    private String beforeMachineId;

    /** 操作前中班计划量 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.beforeDayPlan")
    @ApiModelProperty(value = "操作前中班计划量")
    private Double beforeDayPlan;

    /** 操作前夜班计划量 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.beforeNightPlan")
    @ApiModelProperty(value = "操作前夜班计划量")
    private Double beforeNightPlan;

    /** 操作后机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.afterMachineId")
    @ApiModelProperty(value = "操作后机台ID,多个逗号分割")
    private String afterMachineId;

    /** 操作后中班计划量 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.afterDayPlan")
    @ApiModelProperty(value = "操作后中班计划量")
    private Double afterDayPlan;

    /** 操作后夜班计划量 */
    @Excel(name = "ui.data.column.xwyy.dispatcherlog.afterNightPlan")
    @ApiModelProperty(value = "操作后夜班计划量")
    private Double afterNightPlan;

    private transient String startTime;

    private transient String endTime;

    /**
     * 用于导出转换操作类型字典项
     */
    private Map<String,String> operationTypeDictMap;
}
