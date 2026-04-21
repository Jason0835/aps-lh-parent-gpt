package com.zlt.aps.mdm.api.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 成型精度计划实体（MDM侧复用）
 * 对应表：T_CX_PRECISION_PLAN
 */
@Data
@TableName("T_CX_PRECISION_PLAN")
@ApiModel(value = "成型精度计划(MDM)", description = "成型机台精度维护计划")
public class CxPrecisionPlan extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分厂编码 */
    @TableField("FACTORY_CODE")
    private String factoryCode;

    /** 机台编号 */
    @TableField("MACHINE_CODE")
    private String machineCode;

    /** 精度类型 */
    @TableField("PRECISION_TYPE")
    private String precisionType;

    /** 周期（15/60） */
    @TableField("PRECISION_CYCLE")
    private String precisionCycle;

    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("PLAN_DATE")
    private Date planDate;

    /** 实际执行日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("ACTUAL_DATE")
    private Date actualDate;

    /** 到期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("DUE_DATE")
    private Date dueDate;

    /** 距离计划日期剩余天数 */
    @TableField("DAYS_TO_DUE")
    private Long daysToDue;

    /** 排程日期（硫化排程回填） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("SCHEDULE_DATE")
    private Date scheduleDate;

    /** 上次保养日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("LAST_MAINTENANCE_DATE")
    private Date lastMaintenanceDate;

    /** 完成情况：0-未完成，1-已完成 */
    @TableField("COMPLETION_STATUS")
    private String completionStatus;

    /** 计划年度 */
    @TableField("YEAR")
    private BigDecimal year;

    /** 预警状态：0-未预警，1-已预警 */
    @TableField("WARNING_STATUS")
    private String warningStatus;

    /** 预警触发日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @TableField("WARNING_DATE")
    private Date warningDate;

    /** 是否已发送预警：0-未发送，1-已发送 */
    @TableField("IS_WARNING_SENT")
    private String isWarningSent;

    /** 数据来源：0-MES，1-系统自动生成 */
    @TableField("DATA_SOURCE")
    private String dataSource;

    /** MES来源ID */
    @TableField("MES_SOURCE_ID")
    private Long mesSourceId;

    /** 分公司编码 */
    @TableField("COMPANY_CODE")
    private String companyCode;

    /** 同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("SYNC_TIME")
    private Date syncTime;

    /** 计划日期开始（搜索用，非数据库列） */
    @ApiModelProperty(value = "计划日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date planDateStart;

    /** 计划日期结束（搜索用，非数据库列） */
    @ApiModelProperty(value = "计划日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date planDateEnd;

    /** 实际日期开始（搜索用，非数据库列） */
    @ApiModelProperty(value = "实际日期开始（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date actualDateStart;

    /** 实际日期结束（搜索用，非数据库列） */
    @ApiModelProperty(value = "实际日期结束（搜索用）")
    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date actualDateEnd;

    /** 计划班次（调度兼容，非数据库列） */
    @TableField(exist = false)
    private String planShift;

    /** 预计时长（小时，调度兼容，非数据库列） */
    @TableField(exist = false)
    private BigDecimal estimatedHours;
}

