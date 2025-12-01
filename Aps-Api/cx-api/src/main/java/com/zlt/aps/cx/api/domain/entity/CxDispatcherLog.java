package com.zlt.aps.cx.api.domain.entity;

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
 * 硫化调度员排程操作日志对象 t_cx_dispatcher_log
 *
 * @author Gim
 * @date 2022-02-25
 */
@ApiModel(value = "硫化调度员排程操作日志对象", description = "硫化调度员排程操作日志对象 ")
@Data
@TableName("t_cx_dispatcher_log")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_T_CX_DISPATCHER_LOG")
public class CxDispatcherLog extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_CX_DISPATCHER_LOG */
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "排程记录id")
    private Long scheduleId;

    /** 操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE */
    @Excel(name = "ui.data.column.cx.dispatcherlog.operType")
    @ApiModelProperty(value = "操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.cx.dispatcherlog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 物料编号 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.sapCode")
    @ApiModelProperty(value = "物料编号")
    private String sapCode;

    /** 胎胚代码 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.embryoCode")
    @ApiModelProperty(value = "胎胚代码")
    private String embryoCode;

    /** 胎胚版本 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.embryoVersion")
    @ApiModelProperty(value = "胎胚版本")
    private String embryoVersion;

    /** 操作前硫化机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeLhMachineCode")
    @ApiModelProperty(value = "操作前硫化机台ID,多个逗号分割")
    private String beforeLhMachineCode;

    /** 操作前成型机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeCxMachineCode")
    @ApiModelProperty(value = "操作前成型机台ID,多个逗号分割")
    private String beforeCxMachineCode;

    /** 操作前一班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeClass1Plan")
    @ApiModelProperty(value = "操作前一班计划量")
    private Integer beforeClass1Plan;

    /** 操作前二班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeClass2Plan")
    @ApiModelProperty(value = "操作前二班计划量")
    private Integer beforeClass2Plan;

    /** 操作前三班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeClass3Plan")
    @ApiModelProperty(value = "操作前三班计划量")
    private Integer beforeClass3Plan;

    /** 操作前次日一班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeClass4Plan")
    @ApiModelProperty(value = "操作前次日一班计划量")
    private Integer beforeClass4Plan;

    /** 操作前次日二班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.beforeClass5Plan")
    @ApiModelProperty(value = "操作前次日二班计划量")
    private Integer beforeClass5Plan;

    /** 操作后硫化机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterLhMachineCode")
    @ApiModelProperty(value = "操作后硫化机台ID,多个逗号分割")
    private String afterLhMachineCode;

    /** 操作后成型机台ID,多个逗号分割 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterCxMachineCode")
    @ApiModelProperty(value = "操作后成型机台ID,多个逗号分割")
    private String afterCxMachineCode;

    /** 操作后一班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterClass1Plan")
    @ApiModelProperty(value = "操作后一班计划量")
    private Integer afterClass1Plan;

    /** 操作后二班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterClass2Plan")
    @ApiModelProperty(value = "操作后二班计划量")
    private Integer afterClass2Plan;

    /** 操作后三班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterClass3Plan")
    @ApiModelProperty(value = "操作后三班计划量")
    private Integer afterClass3Plan;


    /** 操作后次日一班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterClass4Plan")
    @ApiModelProperty(value = "操作后次日一班计划量")
    private Integer afterClass4Plan;


    /** 操作后次日二班计划量 */
    @Excel(name = "ui.data.column.cx.dispatcherlog.afterClass5Plan")
    @ApiModelProperty(value = "操作后次日二班计划量")
    private Integer afterClass5Plan;

    private transient String startTime;

    private transient String endTime;

    /**
     * 用于导出转换操作类型字典项
     */
    private Map<String,String> operationTypeDictMap;
}
