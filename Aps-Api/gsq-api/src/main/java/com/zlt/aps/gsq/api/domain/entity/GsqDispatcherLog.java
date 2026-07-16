package com.zlt.aps.gsq.api.domain.entity;

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
 * 钢丝圈调度员排程操作日志对象 t_gsq_dispatcher_log
 *
 * <p>6班次制：
 * <ul>
 *   <li>1班：D日中班(16:00-24:00)</li>
 *   <li>2班：D+1日夜班(00:00-08:00)</li>
 *   <li>3班：D+1日早班(08:00-16:00)</li>
 *   <li>4班：D+1日中班(16:00-24:00)</li>
 *   <li>5班：D+2日夜班(00:00-08:00)</li>
 *   <li>6班：D+2日早班(08:00-16:00)</li>
 * </ul>
 * D = 排程日期 - 2（即今天）
 *
 * @author Gim
 * @date 2022-02-25
 */
@ApiModel(value = "钢丝圈调度员排程操作日志对象", description = "钢丝圈调度员排程操作日志对象 ")
@Data
@TableName("t_gsq_dispatcher_log")
@EqualsAndHashCode(callSuper = false)
@KeySequence(value = "SEQ_T_GSQ_DISPATCHER_LOG", dbType = DbType.ORACLE)
public class GsqDispatcherLog extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键ID，对应自增序列为：SEQ_T_GSQ_DISPATCHER_LOG */
    @ApiModelProperty(value = "id")
    private Long id;

    /** 排程记录id */
    @ApiModelProperty(value = "排程记录id")
    private Long scheduleId;

    /** 操作类型：0--转机台、1--调量、2--插单、3--删除。对应数据字典：DISPATCHER_OPER_TYPE */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.operType")
    @ApiModelProperty(value = "操作类型：0--转机台、1--调量、2--插单、3--删除。对应数据字典：DISPATCHER_OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "ui.data.column.gsq.dispatcherlog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    private Date scheduleDate;

    /** 钢丝圈代码 */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.steelRingCode")
    @ApiModelProperty(value = "钢丝圈代码")
    private String steelRingCode;

    /** 操作前机台编码 */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeMachineCode")
    @ApiModelProperty(value = "操作前机台编码")
    private String beforeMachineCode;

    /** 操作前1班计划量(D日中班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeClass1Plan")
    @ApiModelProperty(value = "操作前1班计划量(D日中班)")
    private Integer beforeClass1Plan;

    /** 操作前2班计划量(D+1日夜班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeClass2Plan")
    @ApiModelProperty(value = "操作前2班计划量(D+1日夜班)")
    private Integer beforeClass2Plan;

    /** 操作前3班计划量(D+1日早班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeClass3Plan")
    @ApiModelProperty(value = "操作前3班计划量(D+1日早班)")
    private Integer beforeClass3Plan;

    /** 操作前4班计划量(D+1日中班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeClass4Plan")
    @ApiModelProperty(value = "操作前4班计划量(D+1日中班)")
    private Integer beforeClass4Plan;

    /** 操作前5班计划量(D+2日夜班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeClass5Plan")
    @ApiModelProperty(value = "操作前5班计划量(D+2日夜班)")
    private Integer beforeClass5Plan;

    /** 操作前6班计划量(D+2日早班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.beforeClass6Plan")
    @ApiModelProperty(value = "操作前6班计划量(D+2日早班)")
    private Integer beforeClass6Plan;

    /** 操作后机台编码 */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterMachineCode")
    @ApiModelProperty(value = "操作后机台编码")
    private String afterMachineCode;

    /** 操作后1班计划量(D日中班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterClass1Plan")
    @ApiModelProperty(value = "操作后1班计划量(D日中班)")
    private Integer afterClass1Plan;

    /** 操作后2班计划量(D+1日夜班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterClass2Plan")
    @ApiModelProperty(value = "操作后2班计划量(D+1日夜班)")
    private Integer afterClass2Plan;

    /** 操作后3班计划量(D+1日早班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterClass3Plan")
    @ApiModelProperty(value = "操作后3班计划量(D+1日早班)")
    private Integer afterClass3Plan;

    /** 操作后4班计划量(D+1日中班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterClass4Plan")
    @ApiModelProperty(value = "操作后4班计划量(D+1日中班)")
    private Integer afterClass4Plan;

    /** 操作后5班计划量(D+2日夜班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterClass5Plan")
    @ApiModelProperty(value = "操作后5班计划量(D+2日夜班)")
    private Integer afterClass5Plan;

    /** 操作后6班计划量(D+2日早班) */
    @Excel(name = "ui.data.column.gsq.dispatcherlog.afterClass6Plan")
    @ApiModelProperty(value = "操作后6班计划量(D+2日早班)")
    private Integer afterClass6Plan;

    private transient String startTime;

    private transient String endTime;

    /**
     * 用于导出转换操作类型字典项
     */
    private Map<String, String> operationTypeDictMap;
}
