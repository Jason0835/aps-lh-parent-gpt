package com.zlt.aps.cx.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成型精度计划实体
 * 对应表：T_CX_PRECISION_PLAN
 */
@Data
@TableName("T_CX_PRECISION_PLAN")
@ApiModel(value = "成型精度计划", description = "成型机台精度维护计划")
public class CxPrecisionPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分厂编码 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.factoryCode", dictType = "biz_factory_name")
    @ApiModelProperty(value = "分厂编码")
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 机台编号 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.machineCode")
    @ApiModelProperty(value = "机台编号")
    @TableField("MACHINE_CODE")
    private String machineCode;

    /** 精度类型 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.accuracyType", dictType = "cx_precision_plan_type")
    @ApiModelProperty(value = "精度类型")
    @TableField("PRECISION_TYPE")
    private String precisionType;

    /** 计划日期 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.planDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "计划日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private LocalDate planDate;

    /** 实际执行日期 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.actualDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "实际执行日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("ACTUAL_DATE")
    private LocalDate actualDate;

    /** 到期日期 */
//    @Excel(name = "ui.data.column.cxPrecisionPlan.dueDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "到期日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private LocalDate dueDate;

    /** 距离计划日期剩余天数 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.daysToDue")
    @ApiModelProperty(value = "距离计划日期剩余天数")
    @TableField("DAYS_TO_DUE")
    private Integer daysToDue;

    /** 上次保养日期 */
//    @Excel(name = "ui.data.column.cxPrecisionPlan.lastMaintenanceDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "上次保养日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_MAINTENANCE_DATE")
    private LocalDate lastMaintenanceDate;

    /** 完成情况：0-未完成，1-已完成 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.completionStatus", dictType = "lh_precision_completion_status")
    @ApiModelProperty(value = "完成情况：0-未完成，1-已完成")
    @TableField("COMPLETION_STATUS")
    private String completionStatus;

    /** 计划年度 */
//    @Excel(name = "ui.data.column.cxPrecisionPlan.year")
    @ApiModelProperty(value = "计划年度")
    @TableField("YEAR")
    private BigDecimal year;

    /** 预警状态：0-未预警，1-已预警 */
//    @Excel(name = "ui.data.column.cxPrecisionPlan.warningStatus", dictType = "lh_precision_warning_status")
    @ApiModelProperty(value = "预警状态：0-未预警，1-已预警")
    @TableField("WARNING_STATUS")
    private String warningStatus;

    /** 预警触发日期 */
//    @Excel(name = "ui.data.column.cxPrecisionPlan.warningDate", dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "预警触发日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("WARNING_DATE")
    private LocalDate warningDate;

    /** 是否已发送预警：0-未发送，1-已发送 */
//    @Excel(name = "ui.data.column.cxPrecisionPlan.isWarningSent", dictType = "lh_precision_warning_sent")
    @ApiModelProperty(value = "是否已发送预警：0-未发送，1-已发送")
    @TableField("IS_WARNING_SENT")
    private String isWarningSent;

    /** 数据来源：0-MES，1-系统自动生成 */
    @Excel(name = "ui.data.column.cxPrecisionPlan.dataSource", dictType = "lh_precision_data_source")
    @ApiModelProperty(value = "数据来源：0-MES，1-系统自动生成")
    @TableField("DATA_SOURCE")
    private String dataSource;

    /** MES来源ID */
    @ApiModelProperty(value = "MES来源ID")
    @TableField("MES_SOURCE_ID")
    private Long mesSourceId;

    /** 分公司编码 */
    @ApiModelProperty(value = "分公司编码")
    @TableField("COMPANY_CODE")
    private String companyCode;

    /** 备注 */
    @Excel(name = "ui.common.column.remark")
    @ApiModelProperty(value = "备注")
    @TableField("REMARK")
    private String remark;

    /** 周期（展示字段，非数据库列） */
    @ApiModelProperty(value = "周期（15/60）")
    @TableField("PRECISION_CYCLE")
    private String precisionCycle;

    /** 排程日期（展示字段，非数据库列） */
    @ApiModelProperty(value = "排程日期")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 计划日期开始（搜索用） */
    @ApiModelProperty(value = "计划日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateStart;

    /** 计划日期结束（搜索用） */
    @ApiModelProperty(value = "计划日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDateEnd;

    /** 实际日期开始（搜索用） */
    @ApiModelProperty(value = "实际日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDateStart;

    /** 实际日期结束（搜索用） */
    @ApiModelProperty(value = "实际日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDateEnd;
}
