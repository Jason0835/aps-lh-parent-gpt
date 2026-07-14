package com.zlt.aps.nc.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 内衬调度员排程操作日志对象 t_nc_dispatcher_log
 * 
 * @author zlt
 * @date 2026-02-25
 */
@ApiModel(value = "内衬调度员排程操作日志对象", description = "内衬调度员排程操作日志对象 ")
@Data
@TableName("T_NC_DISPATCHER_LOG")
@EqualsAndHashCode(callSuper = false)
public class NcDispatcherLog extends ApsBaseEntity{

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "工厂编码")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    @ApiModelProperty(value = "排程记录id")
    @TableField(value = "SCHEDULE_ID")
    private Long scheduleId;

    /** 操作类型：0--转机台、1--调量。对应数据字典：DISPATCHER_OPER_TYPE */
    @Excel(name = "ui.data.column.dj.dispatcherlog.operType", dictType = "DISPATCHER_OPER_TYPE")
    @ApiModelProperty(value = "操作类型")
    @TableField(value = "OPER_TYPE")
    private String operType;

    /** 排程日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "ui.data.column.dj.dispatcherlog.scheduleDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "排程日期")
    @TableField(value = "SCHEDULE_DATE")
    private Date scheduleDate;

    /** 内衬代码 */
    @Excel(name = "ui.data.column.dj.dispatcherlog.materialCode")
    @ApiModelProperty(value = "内衬代码")
    @TableField(value = "MATERIAL_CODE")
    private String materialCode;

    /** 操作前机台ID,多个逗号分割 */
    @ApiModelProperty(value = "操作前机台编号")
    @TableField(value = "BEFORE_MACHINE_CODE")
    private String beforeMachineCode;

    /** 操作前中班计划量 */
    @ApiModelProperty(value = "操作前中班计划量")
    @TableField(value = "BEFORE_CLASS1_PLAN_QTY")
    private BigDecimal beforeClass1PlanQty;

    /** 操作前夜班计划量 */
    @ApiModelProperty(value = "操作前夜班计划量")
    @TableField(value = "BEFORE_CLASS2_PLAN_QTY")
    private BigDecimal beforeClass2PlanQty;

    /** 操作前早班计划量 */
    @ApiModelProperty(value = "操作前早班计划量")
    @TableField(value = "BEFORE_CLASS3_PLAN_QTY")
    private BigDecimal beforeClass3PlanQty;

    /** 操作后机台ID,多个逗号分割 */
    @ApiModelProperty(value = "操作后机台编号")
    @TableField(value = "AFTER_MACHINE_CODE")
    private String afterMachineCode;

    /** 操作后中班计划量 */
    @ApiModelProperty(value = "操作后中班计划量")
    @TableField(value = "AFTER_CLASS1_PLAN_QTY")
    private BigDecimal afterClass1PlanQty;

    /** 操作后夜班计划量 */
    @ApiModelProperty(value = "操作后夜班计划量")
    @TableField(value = "AFTER_CLASS2_PLAN_QTY")
    private BigDecimal afterClass2PlanQty;

    /** 操作后早班计划量 */
    @ApiModelProperty(value = "操作后早班计划量")
    @TableField(value = "AFTER_CLASS3_PLAN_QTY")
    private BigDecimal afterClass3PlanQty;

    @TableField(exist = false)
    private transient String startTime;

    @TableField(exist = false)
    private transient String endTime;

    /**
     * 用于导出转换操作类型字典项
     */
    @TableField(exist = false)
    private Map<String, String> operationTypeDictMap;
}
